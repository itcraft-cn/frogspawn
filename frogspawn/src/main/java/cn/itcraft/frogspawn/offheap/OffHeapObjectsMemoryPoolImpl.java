package cn.itcraft.frogspawn.offheap;

import cn.itcraft.frogspawn.ObjectCreator;
import cn.itcraft.frogspawn.ObjectsMemoryPool;
import sun.misc.Unsafe;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 非堆内存对象池实现
 */
public class OffHeapObjectsMemoryPoolImpl<T extends PrimitiveStructure> implements ObjectsMemoryPool<T> {

    private static final Unsafe UNSAFE = UnsafeUtil.getTheUnsafe();
    private static final int DEFAULT_MAX_POOL_SIZE = 1024;

    private final AtomicLong totalAllocatedMemory = new AtomicLong(0);
    private final ObjectCreator<T> creator;
    private final Object[] objectPool;
    private final AtomicInteger poolSize = new AtomicInteger(0);
    private final int maxPoolSize;

    private static final int THREAD_LOCAL_CACHE_SIZE = 32;

    /**
     * 线程本地缓存，每个线程维护一个对象栈
     */
    private static final ThreadLocal<ThreadLocalCache> THREAD_LOCAL_CACHE =
            ThreadLocal.withInitial(ThreadLocalCache::new);

    /**
     * 内存池相关字段
     */
    // 每个槽位64字节
    private static final int SLOT_SIZE = 64;
    // 4MB内存池
    private static final int MEMORY_POOL_SIZE = 4 * 1024 * 1024;
    // 65536个槽位
    private static final int SLOT_COUNT = MEMORY_POOL_SIZE / SLOT_SIZE;
    // 每次从内存池获取的槽位数量
    private static final int MEMORY_CACHE_BATCH_SIZE = 64;

    // 内存池起始地址
    private final long memoryPoolAddress;
    // 可用槽位栈
    private final long[] freeSlots;
    private final AtomicInteger freeSlotCount = new AtomicInteger(0); // 可用槽位数量

    /**
     * 线程本地内存缓存，每个线程维护一个内存地址栈
     */
    private final ThreadLocal<ThreadLocalMemoryCache> THREAD_LOCAL_MEMORY_CACHE =
            ThreadLocal.withInitial(() -> new ThreadLocalMemoryCache(this));

    public OffHeapObjectsMemoryPoolImpl(ObjectCreator<T> creator) {
        this(creator, DEFAULT_MAX_POOL_SIZE);
    }

    public OffHeapObjectsMemoryPoolImpl(ObjectCreator<T> creator, int maxPoolSize) {
        if (maxPoolSize <= 0) {
            throw new IllegalArgumentException("maxPoolSize must be positive");
        }
        this.creator = creator;
        this.maxPoolSize = maxPoolSize;

        // 初始化对象池数组
        this.objectPool = new Object[maxPoolSize];

        // 验证类结构
        T sample = creator.create();
        ClassChecker.checkClassValid(sample.getClass(), PrimitiveStructure.class);
        sample.setAddress(0); // 释放示例对象

        // 初始化内存池
        this.memoryPoolAddress = UNSAFE.allocateMemory(MEMORY_POOL_SIZE);
        this.freeSlots = new long[SLOT_COUNT];
        // 将所有槽位初始化为可用
        for (int i = 0; i < SLOT_COUNT; i++) {
            freeSlots[i] = memoryPoolAddress + i * SLOT_SIZE;
        }
        freeSlotCount.set(SLOT_COUNT);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T fetch() {
        // 1. 尝试从线程本地缓存获取Java对象
        T obj = THREAD_LOCAL_CACHE.get().poll();
        boolean fromThreadLocalCache = obj != null;
        boolean fromPool = false;

        if (!fromThreadLocalCache) {
            // 2. 尝试从主对象池获取Java对象
            int idx;
            while (true) {
                idx = poolSize.get();
                if (idx <= 0) {
                    obj = creator.create();
                    break;
                }
                if (poolSize.compareAndSet(idx, idx - 1)) {
                    obj = (T) objectPool[idx - 1];
                    // 清除槽位避免内存泄漏
                    objectPool[idx - 1] = null;
                    fromPool = true;
                    break;
                }
            }
        }

        // 3. 检查对象是否已有内存地址（来自线程本地缓存）
        long existingAddress = obj.getAddress();
        if (existingAddress != 0) {
            try {
                allocateArrayMemory(obj);
                obj.writeToMemory();
                return obj;
            } catch (Exception e) {
                cleanupPartialObject(obj, existingAddress, fromThreadLocalCache, fromPool);
                throw new RuntimeException("Failed to create off-heap object", e);
            }
        }

        // 4. 分配对象主体内存
        int objectSize = obj.getObjectSize();
        long objectAddress = allocateMemory(objectSize);

        try {
            // 5. 为数组字段分配内存
            allocateArrayMemory(obj);

            // 6. 设置对象地址
            obj.setAddress(objectAddress);

            // 7. 将对象数据写入内存
            obj.writeToMemory();

            return obj;
        } catch (Exception e) {
            // 如果失败，清理部分初始化的对象
            cleanupPartialObject(obj, objectAddress, fromThreadLocalCache, fromPool);
            throw new RuntimeException("Failed to create off-heap object", e);
        }
    }

    @Override
    public void release(T obj) {
        if (obj == null) {
            return;
        }

        ThreadLocalCache cache = THREAD_LOCAL_CACHE.get();
        if (cache.push(obj)) {
            obj.reset();
            return;
        }

        releaseInternal(obj);
        obj.reset();
        obj.setAddress(0);
        for (int i = 0; i < obj.getArrayFieldCount(); i++) {
            obj.setArrayAddress(i, 0);
        }

        int idx;
        do {
            idx = poolSize.get();
            if (idx >= maxPoolSize) {
                return;
            }
        } while (!poolSize.compareAndSet(idx, idx + 1));
        objectPool[idx] = obj;
    }

    /**
     * 内部释放方法，不重置对象状态
     */
    private void releaseInternal(T obj) {
        long objectAddress = obj.getAddress();
        if (objectAddress == 0) {
            return; // 已经释放
        }

        // 1. 释放数组内存
        freeArrayMemory(obj);

        // 2. 释放对象主体内存
        freeMemory(objectAddress);
    }

    /**
     * 清理部分初始化的对象（用于错误处理）
     *
     * @param obj                  对象
     * @param objectAddress        已分配但可能未设置的对象地址
     * @param fromThreadLocalCache 对象是否来自线程本地缓存
     * @param fromPool             对象是否来自主池
     */
    private void cleanupPartialObject(T obj, long objectAddress, boolean fromThreadLocalCache, boolean fromPool) {
        // 首先清理任何已分配的内存
        if (obj.getAddress() == 0 && objectAddress != 0) {
            freeMemory(objectAddress);
        }

        // 清理数组内存（如果有）
        freeArrayMemory(obj);

        // 清理对象主体内存（如果地址已设置）
        long addr = obj.getAddress();
        if (addr != 0) {
            freeMemory(addr);
        }

        // 重置对象状态
        obj.reset();
        obj.setAddress(0);
        for (int i = 0; i < obj.getArrayFieldCount(); i++) {
            obj.setArrayAddress(i, 0);
        }

        // 如果对象来自线程本地缓存，尝试放回线程本地缓存
        if (fromThreadLocalCache) {
            // 不检查结果，但结果如果是false，则说明
            // -> 线程本地缓存已满，无法放回对象，对象将被丢弃
            THREAD_LOCAL_CACHE.get().push(obj);
        }
        // 如果对象来自主池，尝试放回主池
        else if (fromPool) {
            int idx;
            do {
                idx = poolSize.get();
                if (idx >= maxPoolSize) {
                    // 池已满，无法放回对象
                    // 需要增加计数器以抵消fetch开始时减少的计数
                    poolSize.incrementAndGet();
                    return;
                }
            } while (!poolSize.compareAndSet(idx, idx + 1));
            objectPool[idx] = obj;
        }
    }

    /**
     * 为对象的数组字段分配内存
     *
     * @return 成功分配的数组地址数量，如果失败则抛出异常
     */
    private int allocateArrayMemory(T obj) {
        int arrayFieldCount = obj.getArrayFieldCount();
        for (int i = 0; i < arrayFieldCount; i++) {
            int arrayLength = obj.getArrayLength(i);
            if (arrayLength > 0) {
                // 计算数组数据大小
                // 注意：这里假设数组元素是基本类型，大小由对象自己决定
                // 实际实现中可能需要更复杂的计算
                long arraySize = estimateArraySize(obj, i);
                long arrayAddress = allocateMemory(arraySize);
                try {
                    obj.setArrayAddress(i, arrayAddress);
                } catch (Exception e) {
                    // 如果设置失败，释放刚分配的内存
                    freeMemory(arrayAddress);
                    throw e;
                }
            }
        }
        return arrayFieldCount;
    }

    /**
     * 释放对象的数组内存
     */
    private void freeArrayMemory(T obj) {
        int arrayFieldCount = obj.getArrayFieldCount();
        for (int i = 0; i < arrayFieldCount; i++) {
            long arrayAddress = obj.getArrayAddress(i);
            freeMemory(arrayAddress);
        }
    }

    /**
     * 估算数组大小（简化实现）
     * 实际实现中，对象应该知道数组元素类型和大小
     */
    private long estimateArraySize(T obj, int arrayIndex) {
        int arrayLength = obj.getArrayLength(arrayIndex);
        // 假设最坏情况：double数组（8字节/元素）
        return (long) arrayLength << 3;
    }

    private long allocateMemory(long size) {
        if (size <= 0) {
            return 0;
        }

        // 如果请求大小超过槽位大小，回退到直接分配
        if (size > SLOT_SIZE) {
            long address = UNSAFE.allocateMemory(size);
            totalAllocatedMemory.addAndGet(size);
            return address;
        }

        // 首先尝试从线程本地内存缓存获取
        ThreadLocalMemoryCache cache = THREAD_LOCAL_MEMORY_CACHE.get();
        long address = cache.poll();
        if (address != 0) {
            return address;
        }

        // 线程本地缓存为空且无法从内存池补充，回退到原始内存池分配
        while (true) {
            int count = freeSlotCount.get();
            if (count <= 0) {
                // 内存池已空，回退到直接分配
                long directAddress = UNSAFE.allocateMemory(size);
                totalAllocatedMemory.addAndGet(size);
                return directAddress;
            }
            if (freeSlotCount.compareAndSet(count, count - 1)) {
                long poolAddress = freeSlots[count - 1];
                // 清除槽位引用
                freeSlots[count - 1] = 0;
                // 不清零内存以提高性能，对象应该通过writeToMemory写入数据
                // UNSAFE.setMemory(poolAddress, size, (byte) 0);
                // 注意：这里不更新totalAllocatedMemory，因为内存已预分配
                return poolAddress;
            }
        }
    }

    private void freeMemory(long address) {
        if (address == 0) {
            return;
        }

        // 检查地址是否在内存池范围内
        if (address >= memoryPoolAddress && address < memoryPoolAddress + MEMORY_POOL_SIZE) {
            // 地址在内存池范围内，放入线程本地缓存
            THREAD_LOCAL_MEMORY_CACHE.get().push(address);
        } else {
            // 地址不在内存池范围内，直接释放
            UNSAFE.freeMemory(address);
        }
    }

    /**
     * 获取已分配的总内存大小
     */
    public long getTotalAllocatedMemory() {
        return totalAllocatedMemory.get();
    }

    /**
     * 获取当前对象池大小
     */
    public int getPoolSize() {
        return poolSize.get();
    }

    /**
     * 获取对象池最大大小
     */
    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    /**
     * 线程本地缓存实现
     */
    private static class ThreadLocalCache {
        private final Object[] cache;
        private final long[] memoryAddresses;
        private int size;

        ThreadLocalCache() {
            this.cache = new Object[THREAD_LOCAL_CACHE_SIZE];
            this.memoryAddresses = new long[THREAD_LOCAL_CACHE_SIZE];
            this.size = 0;
        }

        @SuppressWarnings("unchecked")
        <T> T poll() {
            if (size == 0) {
                return null;
            }
            size--;
            T obj = (T) cache[size];
            long address = memoryAddresses[size];
            cache[size] = null;
            memoryAddresses[size] = 0;
            ((PrimitiveStructure) obj).setAddress(address);
            return obj;
        }

        boolean push(Object obj) {
            if (size >= THREAD_LOCAL_CACHE_SIZE) {
                return false;
            }
            cache[size] = obj;
            memoryAddresses[size] = ((PrimitiveStructure) obj).getAddress();
            size++;
            return true;
        }

        int size() {
            return size;
        }
    }

    /**
     * 线程本地内存缓存实现
     */
    private static class ThreadLocalMemoryCache {
        private final OffHeapObjectsMemoryPoolImpl<?> pool;
        private final long[] cache;
        private int size;

        ThreadLocalMemoryCache(OffHeapObjectsMemoryPoolImpl<?> pool) {
            this.pool = pool;
            this.cache = new long[MEMORY_CACHE_BATCH_SIZE * 2]; // 双倍大小以避免频繁批量操作
            this.size = 0;
        }

        /**
         * 从缓存中获取内存地址，如果缓存为空则从内存池批量获取
         */
        long poll() {
            if (size == 0) {
                // 从内存池批量获取槽位
                if (!refillFromPool()) {
                    // 内存池可能已空，返回0表示需要回退到直接分配
                    return 0;
                }
            }
            size--;
            long address = cache[size];
            cache[size] = 0; // 清除引用
            return address;
        }

        /**
         * 将内存地址放入缓存，如果缓存已满则批量返回给内存池
         */
        void push(long address) {
            if (size >= cache.length) {
                // 缓存已满，批量返回给内存池
                flushToPool();
            }
            cache[size] = address;
            size++;
        }

        /**
         * 从内存池批量获取槽位
         *
         * @return 是否成功获取到槽位
         */
        private boolean refillFromPool() {
            // 从内存池获取一批槽位
            int batchSize = Math.min(MEMORY_CACHE_BATCH_SIZE, pool.freeSlotCount.get());
            if (batchSize <= 0) {
                return false;
            }

            // 使用CAS从内存池获取多个槽位
            int acquired = 0;
            while (acquired < batchSize) {
                int count = pool.freeSlotCount.get();
                if (count <= 0) {
                    break;
                }
                int toAcquire = Math.min(batchSize - acquired, count);
                if (pool.freeSlotCount.compareAndSet(count, count - toAcquire)) {
                    // 成功获取toAcquire个槽位
                    for (int i = 0; i < toAcquire; i++) {
                        long address = pool.freeSlots[count - 1 - i];
                        pool.freeSlots[count - 1 - i] = 0;
                        cache[size] = address;
                        size++;
                    }
                    acquired += toAcquire;
                }
            }
            return acquired > 0;
        }

        /**
         * 将缓存中的槽位批量返回给内存池
         */
        private void flushToPool() {
            if (size == 0) {
                return;
            }

            // 批量返回给内存池
            int batchSize = Math.min(size, MEMORY_CACHE_BATCH_SIZE);
            int returned = 0;
            while (returned < batchSize) {
                int count = pool.freeSlotCount.get();
                if (count >= SLOT_COUNT) {
                    // 内存池已满，无法返回（这种情况不应该发生）
                    break;
                }
                int toReturn = Math.min(batchSize - returned, SLOT_COUNT - count);
                if (pool.freeSlotCount.compareAndSet(count, count + toReturn)) {
                    // 成功返回toReturn个槽位
                    for (int i = 0; i < toReturn; i++) {
                        pool.freeSlots[count + i] = cache[size - 1 - i];
                        cache[size - 1 - i] = 0;
                    }
                    size -= toReturn;
                    returned += toReturn;
                }
            }
        }
    }
}

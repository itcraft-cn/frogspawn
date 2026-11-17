package cn.itcraft.frogspawn.offheap;

import cn.itcraft.frogspawn.ObjectCreator;
import cn.itcraft.frogspawn.ObjectsMemoryPool;
import cn.itcraft.frogspawn.platform.MemoryAccess;
import cn.itcraft.frogspawn.platform.PlatformMemory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 非堆内存对象池实现
 * Off-heap memory object pool implementation
 */
public class OffHeapObjectsMemoryPoolImpl<T extends PrimitiveStructure> implements ObjectsMemoryPool<T> {

    private static final MemoryAccess MEMORY = PlatformMemory.ACCESS;
    private static final int DEFAULT_MAX_POOL_SIZE = 1024;

    private final AtomicLong totalAllocatedMemory = new AtomicLong(0);
    private final ObjectCreator<T> creator;
    private final Object[] objectPool;
    private final AtomicInteger poolSize = new AtomicInteger(0);
    private final int maxPoolSize;

    /**
     * 内存池相关字段
     */
    private static final int SLOT_SIZE = 64;
    private static final int MEMORY_POOL_SIZE = 4 * 1024 * 1024;
    private static final int SLOT_COUNT = MEMORY_POOL_SIZE / SLOT_SIZE;

    private final long memoryPoolAddress;
    private final long[] freeSlots;
    private final AtomicInteger freeSlotCount = new AtomicInteger(0);

    /**
     * 合并的对象和内存缓存
     */
    private static final int OBJECT_CACHE_SIZE = 32;
    private static final int MEMORY_CACHE_SIZE = 128;
    
    private static final ThreadLocal<CombinedCache> COMBINED_CACHE =
            ThreadLocal.withInitial(CombinedCache::new);

    public OffHeapObjectsMemoryPoolImpl(ObjectCreator<T> creator) {
        this(creator, DEFAULT_MAX_POOL_SIZE);
    }

    public OffHeapObjectsMemoryPoolImpl(ObjectCreator<T> creator, int maxPoolSize) {
        if (maxPoolSize <= 0) {
            throw new IllegalArgumentException("maxPoolSize must be positive");
        }
        this.creator = creator;
        this.maxPoolSize = maxPoolSize;
        this.objectPool = new Object[maxPoolSize];

        T sample = creator.create();
        ClassChecker.checkClassValid(sample.getClass(), PrimitiveStructure.class);
        sample.setAddress(0);

        this.memoryPoolAddress = MEMORY.allocateMemory(MEMORY_POOL_SIZE);
        this.freeSlots = new long[SLOT_COUNT];
        for (int i = 0; i < SLOT_COUNT; i++) {
            freeSlots[i] = memoryPoolAddress + (long) i * SLOT_SIZE;
        }
        freeSlotCount.set(SLOT_COUNT);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T fetch() {
        CombinedCache cache = COMBINED_CACHE.get();
        
        // 1. 尝试从线程本地缓存获取
        T obj = (T) cache.pollObject();
        boolean fromCache = obj != null;
        
        if (!fromCache) {
            // 2. 尝试从主对象池获取
            int idx;
            while (true) {
                idx = poolSize.get();
                if (idx <= 0) {
                    obj = creator.create();
                    break;
                }
                if (poolSize.compareAndSet(idx, idx - 1)) {
                    obj = (T) objectPool[idx - 1];
                    objectPool[idx - 1] = null;
                    break;
                }
            }
        }

        // 3. 检查对象是否已有内存地址
        long existingAddress = obj.getAddress();
        if (existingAddress != 0) {
            // 对象已有内存，仅处理数组
            int arrayCount = obj.getArrayFieldCount();
            if (arrayCount > 0) {
                try {
                    allocateArrayMemory(obj);
                } catch (Exception e) {
                    cleanupOnError(obj, existingAddress, fromCache);
                    throw new IllegalStateException("Failed to allocate array memory", e);
                }
            }
            // 条件写入内存
            if (obj.hasDataToWrite()) {
                obj.writeToMemory();
            }
            return obj;
        }

        // 4. 分配新内存
        int objectSize = obj.getObjectSize();
        long objectAddress = allocateMemory(objectSize, cache);

        try {
            int arrayCount = obj.getArrayFieldCount();
            if (arrayCount > 0) {
                allocateArrayMemory(obj);
            }
            obj.setAddress(objectAddress);
            if (obj.hasDataToWrite()) {
                obj.writeToMemory();
            }
            return obj;
        } catch (Exception e) {
            cleanupOnError(obj, objectAddress, fromCache);
            throw new IllegalStateException("Failed to create off-heap object", e);
        }
    }

    @Override
    public void release(T obj) {
        if (obj == null) {
            return;
        }

        CombinedCache cache = COMBINED_CACHE.get();
        if (cache.pushObject(obj)) {
            obj.reset();
            return;
        }

        // 缓存已满，释放内存
        releaseInternal(obj);
        obj.reset();
        obj.setAddress(0);
        int arrayCount = obj.getArrayFieldCount();
        for (int i = 0; i < arrayCount; i++) {
            obj.setArrayAddress(i, 0);
        }

        // 放入主池
        int idx;
        do {
            idx = poolSize.get();
            if (idx >= maxPoolSize) {
                return;
            }
        } while (!poolSize.compareAndSet(idx, idx + 1));
        objectPool[idx] = obj;
    }

    private void releaseInternal(T obj) {
        long objectAddress = obj.getAddress();
        if (objectAddress == 0) {
            return;
        }
        freeArrayMemory(obj);
        freeMemory(objectAddress);
    }

    private void cleanupOnError(T obj, long address, boolean fromCache) {
        if (obj.getAddress() == 0 && address != 0) {
            freeMemory(address);
        }
        freeArrayMemory(obj);
        long addr = obj.getAddress();
        if (addr != 0) {
            freeMemory(addr);
        }
        obj.reset();
        obj.setAddress(0);
        int arrayCount = obj.getArrayFieldCount();
        for (int i = 0; i < arrayCount; i++) {
            obj.setArrayAddress(i, 0);
        }
        if (fromCache) {
            COMBINED_CACHE.get().pushObject(obj);
        }
    }

    private void allocateArrayMemory(T obj) {
        int arrayFieldCount = obj.getArrayFieldCount();
        for (int i = 0; i < arrayFieldCount; i++) {
            int arrayLength = obj.getArrayLength(i);
            if (arrayLength > 0) {
                long arraySize = (long) arrayLength * obj.getArrayElementSize(i);
                long arrayAddress = allocateMemory(arraySize, COMBINED_CACHE.get());
                obj.setArrayAddress(i, arrayAddress);
            }
        }
    }

    private void freeArrayMemory(T obj) {
        int arrayFieldCount = obj.getArrayFieldCount();
        for (int i = 0; i < arrayFieldCount; i++) {
            long arrayAddress = obj.getArrayAddress(i);
            freeMemory(arrayAddress);
        }
    }

    private long allocateMemory(long size, CombinedCache cache) {
        if (size <= 0) {
            return 0;
        }

        if (size > SLOT_SIZE) {
            long address = MEMORY.allocateMemory(size);
            totalAllocatedMemory.addAndGet(size);
            return address;
        }

        // 从合并缓存获取内存地址
        long address = cache.pollAddress();
        if (address != 0) {
            return address;
        }

        // 从共享内存池获取
        while (true) {
            int count = freeSlotCount.get();
            if (count <= 0) {
                long directAddress = MEMORY.allocateMemory(size);
                totalAllocatedMemory.addAndGet(size);
                return directAddress;
            }
            if (freeSlotCount.compareAndSet(count, count - 1)) {
                return freeSlots[count - 1];
            }
        }
    }

    private void freeMemory(long address) {
        if (address == 0) {
            return;
        }
        if (address >= memoryPoolAddress && address < memoryPoolAddress + MEMORY_POOL_SIZE) {
            COMBINED_CACHE.get().pushAddress(address);
        } else {
            MEMORY.freeMemory(address);
        }
    }

    public long getTotalAllocatedMemory() {
        return totalAllocatedMemory.get();
    }

    public int getPoolSize() {
        return poolSize.get();
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    /**
     * 合并的对象和内存地址缓存
     */
    private static class CombinedCache {
        private final Object[] objectCache;
        private final long[] addressCache;
        private int objectSize;
        private int addressSize;

        CombinedCache() {
            this.objectCache = new Object[OBJECT_CACHE_SIZE];
            this.addressCache = new long[MEMORY_CACHE_SIZE];
            this.objectSize = 0;
            this.addressSize = 0;
        }

        @SuppressWarnings("unchecked")
        <T> T pollObject() {
            if (objectSize == 0) {
                return null;
            }
            objectSize--;
            T obj = (T) objectCache[objectSize];
            objectCache[objectSize] = null;
            long address = addressCache[objectSize];
            if (obj != null) {
                ((PrimitiveStructure) obj).setAddress(address);
            }
            return obj;
        }

        boolean pushObject(Object obj) {
            if (objectSize >= OBJECT_CACHE_SIZE) {
                return false;
            }
            objectCache[objectSize] = obj;
            if (obj instanceof PrimitiveStructure) {
                addressCache[objectSize] = ((PrimitiveStructure) obj).getAddress();
            } else {
                addressCache[objectSize] = 0;
            }
            objectSize++;
            return true;
        }

        long pollAddress() {
            if (addressSize == 0) {
                return 0;
            }
            addressSize--;
            long address = addressCache[MEMORY_CACHE_SIZE - 1 - addressSize];
            addressCache[MEMORY_CACHE_SIZE - 1 - addressSize] = 0;
            return address;
        }

        void pushAddress(long address) {
            if (addressSize >= MEMORY_CACHE_SIZE) {
                return;
            }
            addressCache[MEMORY_CACHE_SIZE - 1 - addressSize] = address;
            addressSize++;
        }
    }
}
package cn.itcraft.frogspawn.offheap;

import cn.itcraft.frogspawn.ObjectCreator;
import cn.itcraft.frogspawn.ObjectsMemoryPool;
import sun.misc.Unsafe;

import java.lang.reflect.Array;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 固定非堆对象池
 * Fixed off-heap object pool
 *
 * @author Helly Guo
 * Created on 2026-03-24
 */
public class FixedOffHeapPool<T extends FixedOffHeapObject> implements ObjectsMemoryPool<T> {

    private static final Unsafe UNSAFE = UnsafeUtil.getTheUnsafe();
    private static final int LOCAL_STACK_SIZE = 64;

    private final ObjectCreator<T> creator;
    private final T[] objects;
    private final int capacity;
    private final int objectSize;
    private final long memoryBlock;

    private final int[] sharedStack;
    private final AtomicInteger sharedCount;

    private final ThreadLocal<LocalCache> localCache;

    @SuppressWarnings("unchecked")
    public FixedOffHeapPool(ObjectCreator<T> creator, int capacity, int objectSize) {
        this.creator = creator;
        this.capacity = capacity;
        this.objectSize = objectSize;

        this.memoryBlock = UNSAFE.allocateMemory((long) capacity * objectSize);

        T sample = creator.create();
        sample.index(0);
        sample.address(memoryBlock);
        
        this.objects = (T[]) Array.newInstance(sample.getClass(), capacity);
        this.objects[0] = sample;
        
        for (int i = 1; i < capacity; i++) {
            T obj = creator.create();
            obj.index(i);
            obj.address(memoryBlock + (long) i * objectSize);
            objects[i] = obj;
        }

        this.sharedStack = new int[capacity];
        for (int i = 0; i < capacity; i++) {
            sharedStack[i] = i;
        }
        this.sharedCount = new AtomicInteger(capacity);

        this.localCache = ThreadLocal.withInitial(LocalCache::new);
    }

    @Override
    public T fetch() {
        LocalCache cache = localCache.get();
        
        if (cache.size > 0) {
            cache.size--;
            return objects[cache.stack[cache.size]];
        }

        if (refillLocalStack(cache)) {
            cache.size--;
            return objects[cache.stack[cache.size]];
        }

        return creator.create();
    }

    @Override
    public void release(T obj) {
        if (obj == null) {
            return;
        }
        
        obj.reset();

        LocalCache cache = localCache.get();

        if (cache.size < LOCAL_STACK_SIZE) {
            cache.stack[cache.size] = obj.index();
            cache.size++;
        } else {
            flushLocalStack(cache);
            cache.stack[0] = obj.index();
            cache.size = 1;
        }
    }

    private boolean refillLocalStack(LocalCache cache) {
        int toFetch = Math.min(LOCAL_STACK_SIZE / 2, sharedCount.get());

        if (toFetch <= 0) return false;

        while (true) {
            int current = sharedCount.get();
            if (current <= 0) return false;

            int fetch = Math.min(toFetch, current);
            if (sharedCount.compareAndSet(current, current - fetch)) {
                for (int i = 0; i < fetch; i++) {
                    cache.stack[i] = sharedStack[current - 1 - i];
                }
                cache.size = fetch;
                return true;
            }
        }
    }

    private void flushLocalStack(LocalCache cache) {
        int count = cache.size;

        while (true) {
            int current = sharedCount.get();
            if (current >= capacity) return;

            int toFlush = Math.min(count, capacity - current);
            if (sharedCount.compareAndSet(current, current + toFlush)) {
                for (int i = 0; i < toFlush; i++) {
                    sharedStack[current + i] = cache.stack[count - 1 - i];
                }
                cache.size = count - toFlush;
                return;
            }
        }
    }

    public int getCapacity() {
        return capacity;
    }

    public int getAvailableCount() {
        return sharedCount.get();
    }

    private static class LocalCache {
        final int[] stack = new int[LOCAL_STACK_SIZE];
        int size = 0;
    }
}
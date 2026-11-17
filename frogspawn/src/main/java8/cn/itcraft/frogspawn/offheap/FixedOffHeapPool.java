package cn.itcraft.frogspawn.offheap;

import cn.itcraft.frogspawn.ObjectCreator;
import cn.itcraft.frogspawn.ObjectsMemoryPool;
import sun.misc.Unsafe;

import java.lang.reflect.Array;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 固定非堆对象池 - 极简优化版
 * Fixed off-heap object pool - Minimal optimized
 *
 * @author Helly Guo
 * Created on 2026-03-24
 */
public class FixedOffHeapPool<T extends FixedOffHeapObject> implements ObjectsMemoryPool<T> {

    private static final Unsafe UNSAFE = UnsafeUtil.getTheUnsafe();
    private static final int CACHE_SIZE = 64;

    private final ObjectCreator<T> creator;
    private final T[] objects;
    private final int capacity;

    private final AtomicInteger stackTop = new AtomicInteger(0);
    private final int[] stack;

    private static class LocalCache {
        final int[] indices = new int[CACHE_SIZE];
        int count = 0;
    }

    private final ThreadLocal<LocalCache> localCache = ThreadLocal.withInitial(LocalCache::new);

    @SuppressWarnings("unchecked")
    public FixedOffHeapPool(ObjectCreator<T> creator, int capacity, int objectSize) {
        this.creator = creator;
        this.capacity = capacity;
        long memoryBlock = UNSAFE.allocateMemory((long) capacity * objectSize);

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

        this.stack = new int[capacity];
        for (int i = 0; i < capacity; i++) {
            stack[i] = i;
        }
        this.stackTop.set(capacity);
    }

    @Override
    public T fetch() {
        LocalCache cache = localCache.get();
        
        if (cache.count > 0) {
            cache.count--;
            return objects[cache.indices[cache.count]];
        }

        int top = stackTop.get();
        if (top <= 0) {
            return creator.create();
        }

        int toFetch = Math.min(CACHE_SIZE, top);
        int newTop = top - toFetch;
        
        if (stackTop.compareAndSet(top, newTop)) {
            for (int i = 0; i < toFetch; i++) {
                cache.indices[i] = stack[newTop + i];
            }
            cache.count = toFetch - 1;
            return objects[cache.indices[toFetch - 1]];
        }
        
        return fetchSlowPath();
    }

    private T fetchSlowPath() {
        LocalCache cache = localCache.get();
        
        for (int retry = 0; retry < 10; retry++) {
            int top = stackTop.get();
            if (top <= 0) {
                return creator.create();
            }

            int toFetch = Math.min(CACHE_SIZE, top);
            int newTop = top - toFetch;
            
            if (stackTop.compareAndSet(top, newTop)) {
                for (int i = 0; i < toFetch; i++) {
                    cache.indices[i] = stack[newTop + i];
                }
                cache.count = toFetch - 1;
                return objects[cache.indices[toFetch - 1]];
            }
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

        if (cache.count < CACHE_SIZE) {
            cache.indices[cache.count] = obj.index();
            cache.count++;
            return;
        }

        int top = stackTop.get();
        int canPut = capacity - top;
        int toPut = Math.min(CACHE_SIZE, canPut);
        
        if (toPut > 0 && stackTop.compareAndSet(top, top + toPut)) {
            for (int i = 0; i < toPut; i++) {
                stack[top + i] = cache.indices[CACHE_SIZE - 1 - i];
            }
            cache.count = CACHE_SIZE - toPut;
            cache.indices[cache.count] = obj.index();
            cache.count++;
        } else {
            cache.indices[CACHE_SIZE - 1] = obj.index();
        }
    }

    public int getCapacity() {
        return capacity;
    }

    public int getAvailableCount() {
        return stackTop.get();
    }
}
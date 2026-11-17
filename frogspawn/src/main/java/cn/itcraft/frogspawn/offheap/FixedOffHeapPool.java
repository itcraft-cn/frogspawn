package cn.itcraft.frogspawn.offheap;

import cn.itcraft.frogspawn.ObjectCreator;
import cn.itcraft.frogspawn.ObjectsMemoryPool;
import cn.itcraft.frogspawn.platform.MemoryAccess;
import cn.itcraft.frogspawn.platform.PlatformMemory;

import java.lang.reflect.Array;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

/**
 * 固定非堆对象池 - 极简优化版
 * Fixed off-heap object pool - Minimal optimized
 *
 * @author Helly Guo
 * Created on 2026-03-24
 */
public class FixedOffHeapPool<T extends FixedOffHeapObject>
        implements ObjectsMemoryPool<T>, AutoCloseable {

    private static final MemoryAccess MEMORY = PlatformMemory.ACCESS;
    private static final int CACHE_SIZE = 64;
    private static final int AVAILABLE = 0;
    private static final int IN_USE = 1;

    private final T[] objects;
    private final int capacity;
    private final long memoryBlock;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicIntegerArray states;
    private final Object stackLock = new Object();

    private final AtomicInteger stackTop = new AtomicInteger(0);
    private final int[] stack;

    private static class LocalCache {
        final int[] indices = new int[CACHE_SIZE];
        int count = 0;
    }

    private final ThreadLocal<LocalCache> localCache = ThreadLocal.withInitial(LocalCache::new);

    @SuppressWarnings("unchecked")
    public FixedOffHeapPool(ObjectCreator<T> creator, int capacity, int objectSize) {
        if (creator == null) {
            throw new IllegalArgumentException("creator must not be null");
        }
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        if (objectSize <= 0) {
            throw new IllegalArgumentException("objectSize must be positive");
        }
        this.capacity = capacity;
        this.states = new AtomicIntegerArray(capacity);
        this.memoryBlock = MEMORY.allocateMemory((long) capacity * objectSize);

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
        checkOpen();
        LocalCache cache = localCache.get();

        if (cache.count > 0) {
            cache.count--;
            return take(cache.indices[cache.count]);
        }

        return fetchBatch(cache);
    }

    private T fetchBatch(LocalCache cache) {
        synchronized (stackLock) {
            int top = stackTop.get();
            if (top <= 0) {
                throw new IllegalStateException("FixedOffHeapPool exhausted, capacity=" + capacity);
            }
            int toFetch = Math.min(CACHE_SIZE, top);
            int newTop = top - toFetch;
            for (int i = 0; i < toFetch; i++) {
                cache.indices[i] = stack[newTop + i];
            }
            stackTop.set(newTop);
            cache.count = toFetch - 1;
            return take(cache.indices[toFetch - 1]);
        }
    }

    private T take(int index) {
        if (!states.compareAndSet(index, AVAILABLE, IN_USE)) {
            throw new IllegalStateException("FixedOffHeapPool state corrupted, index=" + index);
        }
        return objects[index];
    }

    @Override
    public void release(T obj) {
        checkOpen();
        if (obj == null) {
            return;
        }

        int index = obj.index();
        if (index < 0 || index >= capacity || objects[index] != obj) {
            throw new IllegalArgumentException("Object does not belong to this pool, index=" + index);
        }
        if (!states.compareAndSet(index, IN_USE, AVAILABLE)) {
            throw new IllegalArgumentException("Object already released or not fetched, index=" + index);
        }

        obj.reset();

        LocalCache cache = localCache.get();
        if (cache.count < CACHE_SIZE) {
            cache.indices[cache.count] = index;
            cache.count++;
            return;
        }

        pushBatch(cache, index);
    }

    private void pushBatch(LocalCache cache, int releasedIndex) {
        synchronized (stackLock) {
            int top = stackTop.get();
            int canPut = capacity - top;
            if (canPut <= 0) {
                throw new IllegalStateException("FixedOffHeapPool overflow, invalid release, index=" + releasedIndex);
            }
            int toPut = Math.min(CACHE_SIZE, canPut);
            for (int i = 0; i < toPut; i++) {
                stack[top + i] = cache.indices[CACHE_SIZE - 1 - i];
            }
            cache.count = CACHE_SIZE - toPut;
            cache.indices[cache.count] = releasedIndex;
            cache.count++;
            stackTop.set(top + toPut);
        }
    }

    public int getCapacity() {
        return capacity;
    }

    public int getAvailableCount() {
        return stackTop.get();
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            MEMORY.freeMemory(memoryBlock);
        }
    }

    private void checkOpen() {
        if (closed.get()) {
            throw new IllegalStateException("FixedOffHeapPool is closed");
        }
    }
}

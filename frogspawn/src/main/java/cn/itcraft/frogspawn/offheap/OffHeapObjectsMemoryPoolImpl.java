package cn.itcraft.frogspawn.offheap;

import cn.itcraft.frogspawn.ObjectCreator;
import cn.itcraft.frogspawn.ObjectsMemoryPool;
import cn.itcraft.frogspawn.strategy.PoolStrategy;
import sun.misc.Unsafe;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 非堆内存对象池实现
 */
public class OffHeapObjectsMemoryPoolImpl<T extends PrimitiveStructure> implements ObjectsMemoryPool<T> {

    private static final Unsafe UNSAFE = UnsafeUtil.getTheUnsafe();
    private final AtomicLong addressCounter = new AtomicLong(0);
    private final ObjectCreator<T> creator;
    private final PoolStrategy strategy;

    public OffHeapObjectsMemoryPoolImpl(ObjectCreator<T> creator, PoolStrategy strategy) {
        this.creator = creator;
        this.strategy = strategy;
    }

    @Override
    public T fetch() {
        long address = allocateDirect(strategy.getObjectSize());
        T obj = creator.create();
        obj.setAddress(address);
        return obj;
    }

    @Override
    public void release(T obj) {
        if (obj != null) {
            freeDirect(obj.getAddress());
            obj.setAddress(0);
        }
    }

    private long allocateDirect(long size) {
        long address = UNSAFE.allocateMemory(size);
        addressCounter.addAndGet(size);
        return address;
    }

    private void freeDirect(long address) {
        if (address != 0) {
            UNSAFE.freeMemory(address);
        }
    }
}
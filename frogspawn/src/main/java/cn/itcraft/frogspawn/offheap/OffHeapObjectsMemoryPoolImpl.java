package cn.itcraft.frogspawn.offheap;

import cn.itcraft.frogspawn.ObjectCreator;
import cn.itcraft.frogspawn.ObjectsMemoryPool;
import sun.misc.Unsafe;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 非堆内存对象池实现
 */
public class OffHeapObjectsMemoryPoolImpl<T extends PrimitiveStructure> implements ObjectsMemoryPool<T> {

    private static final Unsafe UNSAFE = UnsafeUtil.getTheUnsafe();
    private final AtomicLong totalAllocatedMemory = new AtomicLong(0);
    private final ObjectCreator<T> creator;

    public OffHeapObjectsMemoryPoolImpl(ObjectCreator<T> creator) {
        this.creator = creator;
        
        // 验证类结构
        T sample = creator.create();
        ClassChecker.checkClassValid(sample.getClass(), PrimitiveStructure.class);
        sample.setAddress(0); // 释放示例对象
    }

    @Override
    public T fetch() {
        // 1. 创建Java对象
        T obj = creator.create();
        
        // 2. 分配对象主体内存
        int objectSize = obj.getObjectSize();
        long objectAddress = allocateMemory(objectSize);
        
        try {
            // 3. 为数组字段分配内存
            allocateArrayMemory(obj);
            
            // 4. 设置对象地址
            obj.setAddress(objectAddress);
            
            // 5. 将对象数据写入内存
            obj.writeToMemory();
            
            return obj;
        } catch (Exception e) {
            // 如果失败，释放已分配的内存
            releaseInternal(obj);
            throw new RuntimeException("Failed to create off-heap object", e);
        }
    }

    @Override
    public void release(T obj) {
        if (obj == null) {
            return;
        }
        
        releaseInternal(obj);
        
        // 重置对象状态
        obj.reset();
        obj.setAddress(0);
        for (int i = 0; i < obj.getArrayFieldCount(); i++) {
            obj.setArrayAddress(i, 0);
        }
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
     * 为对象的数组字段分配内存
     */
    private void allocateArrayMemory(T obj) {
        int arrayFieldCount = obj.getArrayFieldCount();
        for (int i = 0; i < arrayFieldCount; i++) {
            int arrayLength = obj.getArrayLength(i);
            if (arrayLength > 0) {
                // 计算数组数据大小
                // 注意：这里假设数组元素是基本类型，大小由对象自己决定
                // 实际实现中可能需要更复杂的计算
                long arraySize = estimateArraySize(obj, i);
                long arrayAddress = allocateMemory(arraySize);
                obj.setArrayAddress(i, arrayAddress);
            }
        }
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
        return (long) arrayLength * 8L;
    }

    private long allocateMemory(long size) {
        if (size <= 0) {
            return 0;
        }
        long address = UNSAFE.allocateMemory(size);
        totalAllocatedMemory.addAndGet(size);
        return address;
    }

    private void freeMemory(long address) {
        if (address != 0) {
            UNSAFE.freeMemory(address);
        }
    }

    /**
     * 获取已分配的总内存大小
     */
    public long getTotalAllocatedMemory() {
        return totalAllocatedMemory.get();
    }
}
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the License); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an AS IS BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.itcraft.frogspawn.impl;

import cn.itcraft.frogspawn.ObjectCreator;
import cn.itcraft.frogspawn.ObjectsMemoryPool;
import cn.itcraft.frogspawn.Resettable;
import cn.itcraft.frogspawn.constants.Constants;
import cn.itcraft.frogspawn.data.WrappedResettable;
import cn.itcraft.frogspawn.misc.PaddedAtomicLong;
import cn.itcraft.frogspawn.misc.SoftRefStore;
import cn.itcraft.frogspawn.util.ArrayUtil;

/**
 * 基于线程本地缓存和环形数组的抽象对象池实现
 * Abstract object pool implementation based on thread-local cache and circular array
 *
 * @author Helly Guo
 * <p>
 * Created on 8/24/21 11:34 PM
 */
public abstract class AbstractCachedPool<T extends Resettable> implements ObjectsMemoryPool<T> {

    /**
     * 线程本地存储的软引用队列，用于快速对象存取
     * Thread-local soft reference store for fast object access
     */
    protected static final ThreadLocal<SoftRefStore<Resettable>> LOCAL_QUEUE =
            ThreadLocal.withInitial(SoftRefStore::new);

    /**
     * 原子指针，用于环形数组的遍历访问
     * Atomic pointer for circular array traversal
     */
    protected final PaddedAtomicLong walker = new PaddedAtomicLong(0);
    
    /**
     * 核心存储数组，包装可重置对象
     * Core storage array wrapping resettable objects
     */
    @SuppressWarnings("rawtypes")
    protected final WrappedResettable[] array;
    
    /**
     * 下标掩码，用于快速计算环形数组索引
     * Index mask for fast circular array index calculation
     */
    protected final int indexMask;

    /**
     * 构造方法，初始化对象池
     * Constructor, initializes the object pool
     * 
     * @param creator 对象创建器 / Object creator
     * @param size 初始容量 / Initial capacity
     */
    AbstractCachedPool(ObjectCreator<T> creator, int size) {
        // 计算最接近的2的幂次方容量
        // Calculate nearest power of two capacity
        int calculatedCapacity = ArrayUtil.findNextPositivePowerOfTwo(size);
        int capacity = Math.min(calculatedCapacity, Constants.MAX_CAPACITY);
        
        // 使用掩码优化索引计算（替代取模运算）
        // Use mask for optimized index calculation (replaces modulo operation)
        indexMask = capacity - 1;
        
        // 创建带缓存行填充的数组（避免伪共享）
        // Create array with cache line padding (prevents false sharing)
        array = ArrayUtil.createArray(WrappedResettable.class, capacity);
        
        WrappedResettable<T> wrapped;
        int paddedCapacity = ArrayUtil.BUFFER_PAD + capacity;
        // 初始化数组元素，跳过缓存填充区域
        // Initialize array elements, skip buffer padding area
        for (int i = ArrayUtil.BUFFER_PAD; i < paddedCapacity; i++) {
            wrapped = new WrappedResettable<>(creator.create());
            // 标记对象在数组中的位置 / Mark object's position in array
            wrapped.getObj().markId(i);
            array[i] = wrapped;
        }
    }

    /**
     * 获取对象（优先从线程本地缓存获取）
     * Fetch object (preferentially from thread-local cache)
     */
    @SuppressWarnings("unchecked")
    @Override
    public T fetch() {
        T t = (T) LOCAL_QUEUE.get().fetch();
        if (t == null || t.isInvalid()) {
            // 缓存未命中时从主池获取 / Fetch from main pool when cache miss
            return fetchData();
        }
        return t;
    }

    /**
     * 抽象方法 - 从主池获取对象的具体实现
     * Abstract method - concrete implementation for fetching from main pool
     */
    protected abstract T fetchData();

    /**
     * 释放对象到线程本地缓存
     * Release object back to thread-local cache
     * 
     * @param used 已使用的对象 / The used object to release
     */
    @Override
    public void release(T used) {
        if (LOCAL_QUEUE.get().release(used)) {
            // 成功释放后执行后续处理 / Perform post-release processing
            wrapRelease(used);
        }
    }

    /**
     * 对象释放后的处理逻辑
     * Post-release processing logic
     * 
     * @param used 已释放的对象 / The released object
     */
    protected void wrapRelease(T used) {
        // 重置对象状态 / Reset object state
        used.reset();
        int id = used.getMarkedId();
        if (id >= 0) {
            // 原子标记对象为未使用状态
            // Atomically mark object as unused
            array[id].getUsed().compareAndSet(true, false);
        }
    }
}

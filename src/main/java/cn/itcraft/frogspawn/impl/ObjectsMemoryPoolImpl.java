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
import cn.itcraft.frogspawn.strategy.FetchFailStrategy;
import cn.itcraft.frogspawn.strategy.PoolStrategy;
import cn.itcraft.frogspawn.util.ArrayUtil;

/**
 * 基于线程本地缓存和环形数组的抽象对象池实现
 * Abstract object pool implementation based on thread-local cache and circular array
 *
 * @author Helly Guo
 * <p>
 * Created on 8/24/21 11:34 PM
 */
public class ObjectsMemoryPoolImpl<T extends Resettable> implements ObjectsMemoryPool<T> {

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
     * 对象创建器，用于生成新的池对象实例
     * <p>
     * Object creator for generating new pool object instances
     */
    private final ObjectCreator<T> creator;

    /**
     * 数据获取失败时的处理策略
     * <p>
     * Strategy for handling data fetch failures
     */
    private final FetchFailStrategy fetchFailStrategy;

    private final Fetcher<T> fetcher;
    private final Releaser<T> releaser;

    /**
     * 构造方法，初始化对象池
     * Constructor, initializes the object pool
     *
     * @param creator 对象创建器 / Object creator
     * @param size    初始容量 / Initial capacity
     */
    public ObjectsMemoryPoolImpl(ObjectCreator<T> creator, int size, PoolStrategy poolStrategy) {
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
        this.creator = creator;
        if (poolStrategy.isPrefetch()) {
            this.fetcher = this::fetchWithPrefetch;
            this.releaser = this::releaseDirect;
            this.fetchFailStrategy = null;
        } else {
            this.fetcher = this::fetchWithCache;
            this.releaser = this::releaseLocalFirst;
            this.fetchFailStrategy = poolStrategy.getFetchFailStrategy();
        }
    }

    /**
     * 从池中获取可用对象（核心方法）
     * Fetch an available object from the pool (core method)
     *
     * @return 可复用的对象实例
     * Reusable object instance
     */
    @Override
    public T fetch() {
        return fetcher.fetch();
    }

    /**
     * 获取对象（优先从线程本地缓存获取）
     * Fetch object (preferentially from thread-local cache)
     */
    @SuppressWarnings("unchecked")
    private T fetchWithCache() {
        T t = (T) LOCAL_QUEUE.get().fetch();
        if (t == null || t.isInvalid()) {
            // 缓存未命中时从主池获取 / Fetch from the main pool when cache missed
            return fetchDataWithTimes();
        }
        return t;
    }

    /**
     * 从池中获取可用对象（核心方法）
     * Fetch an available object from the pool (core method)
     *
     * @return 可复用的对象实例
     * Reusable object instance
     */
    @SuppressWarnings("unchecked")
    private T fetchWithPrefetch() {
        // 获取线程本地软引用存储
        // Get thread-local soft reference store
        SoftRefStore<Resettable> softRefStore = LOCAL_QUEUE.get();

        // 尝试从本地队列获取对象
        // Attempt to get object from local queue
        T t = (T) softRefStore.fetch();

        // 对象无效或不存在时的处理逻辑
        // Handle case when object is invalid or not found
        if (t == null || t.isInvalid()) {
            // 从数据源获取新对象
            // Fetch new object from data source
            t = fetchDataWithLoop();

            // 触发预取机制补充缓存
            // Trigger prefetch mechanism to replenish cache
            prefetch(softRefStore);
        }
        return t;
    }

    /**
     * 从主池获取对象的具体实现
     * concrete implementation for fetching from main pool
     */
    protected T fetchDataWithTimes() {
        return FetchHelper.fetchDataOrFailover(
                // 对象存储数组 | Object storage array
                array,
                // 索引掩码用于快速取模 | Index mask for fast modulo operation
                indexMask,
                // 数组遍历辅助工具 | Array traversal helper
                walker,
                // 失败处理策略 | Failure handling strategy
                fetchFailStrategy,
                // 对象创建器 | Object creator
                creator);
    }

    /**
     * 从主池获取对象的具体实现
     * concrete implementation for fetching from main pool
     */
    protected T fetchDataWithLoop() {
        return FetchHelper.loopFetchData(array, indexMask, walker);
    }

    /**
     * 预填充缓存队列（私有工具方法）
     * Prefill the cache queue (private utility method)
     *
     * @param softRefStore 线程本地软引用存储对象
     *                     Thread-local soft reference store object
     */
    private void prefetch(SoftRefStore<Resettable> softRefStore) {
        // 当缓存为空时进行预填充
        // Prefill when cache is empty
        if (softRefStore.isEmpty()) {
            // 按照预设容量填充缓存
            // Fill cache according to predefined capacity
            for (int i = 0; i < Constants.CACHE_CAPACITY; i++) {
                // 获取新对象并放入缓存
                // Fetch new object and add to cache
                softRefStore.release(fetchDataWithLoop());
            }
        }
    }

    /**
     * 释放并回收对象到池中（核心方法）
     * Release and recycle object back to pool (core method)
     *
     * @param used 已使用的对象实例
     *             Used object instance
     */
    @Override
    public void release(T used) {
        releaser.release(used);
    }

    /**
     * 释放对象到线程本地缓存
     * Release object back to thread-local cache
     *
     * @param used 已使用的对象 / The used object to release
     */
    private void releaseLocalFirst(T used) {
        if (LOCAL_QUEUE.get().release(used)) {
            // 成功释放后执行后续处理 / Perform post-release processing
            wrapRelease(used);
        }
    }

    /**
     * 释放并回收对象到池中（核心方法）
     * Release and recycle object back to pool (core method)
     *
     * @param used 已使用的对象实例
     *             Used object instance
     */
    private void releaseDirect(T used) {
        // 执行实际的释放操作
        // Perform actual release operation
        wrapRelease(used);
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

    private interface Fetcher<T> {
        T fetch();
    }

    private interface Releaser<T> {
        void release(T used);
    }
}

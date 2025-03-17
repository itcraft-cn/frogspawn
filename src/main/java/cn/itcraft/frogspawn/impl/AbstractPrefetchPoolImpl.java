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
import cn.itcraft.frogspawn.Resettable;
import cn.itcraft.frogspawn.constants.Constants;
import cn.itcraft.frogspawn.misc.SoftRefStore;

/**
 * 抽象预取池实现类，用于管理可复用对象的预取和缓存机制
 * Abstract prefetch pool implementation for managing object prefetching and caching of reusable objects
 *
 * @param <T> 泛型类型，需继承Resettable接口，表示可重置的对象
 *           Generic type that extends Resettable interface, representing resettable objects
 * @author Helly Guo
 * <p>
 * Created on 8/24/21 11:34 PM
 */
public abstract class AbstractPrefetchPoolImpl<T extends Resettable> extends AbstractCachedPool<T> {

    /**
     * 构造函数，初始化对象池
     * Constructor for initializing the object pool
     * 
     * @param creator 对象创建器，用于生成新的对象实例
     *               Object creator for generating new instances
     * @param size    对象池的初始容量
     *               Initial capacity of the object pool
     */
    public AbstractPrefetchPoolImpl(ObjectCreator<T> creator, int size) {
        super(creator, size);
    }

    /**
     * 从池中获取可用对象（核心方法）
     * Fetch an available object from the pool (core method)
     * 
     * @return 可复用的对象实例
     *         Reusable object instance
     */
    @SuppressWarnings("unchecked")
    @Override
    public T fetch() {
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
            t = fetchData();
            
            // 触发预取机制补充缓存
            // Trigger prefetch mechanism to replenish cache
            prefetch(softRefStore);
        }
        return t;
    }

    /**
     * 预填充缓存队列（私有工具方法）
     * Prefill the cache queue (private utility method)
     * 
     * @param softRefStore 线程本地软引用存储对象
     *                    Thread-local soft reference store object
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
                softRefStore.release(fetchData());
            }
        }
    }

    /**
     * 释放并回收对象到池中（覆盖父类方法）
     * Release and recycle object back to pool (override parent method)
     * 
     * @param used 已使用的对象实例
     *            Used object instance
     */
    @Override
    public void release(T used) {
        // 重置对象状态到初始值
        // Reset object state to initial values
        used.reset();
        
        // 执行实际的释放操作
        // Perform actual release operation
        wrapRelease(used);
    }
}

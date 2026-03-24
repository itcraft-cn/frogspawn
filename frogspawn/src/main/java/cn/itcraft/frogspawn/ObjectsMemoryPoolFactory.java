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
package cn.itcraft.frogspawn;

import cn.itcraft.frogspawn.local.LocalObjectsMemoryPoolImpl;
import cn.itcraft.frogspawn.offheap.FixedOffHeapObject;
import cn.itcraft.frogspawn.offheap.FixedOffHeapPool;
import cn.itcraft.frogspawn.offheap.OffHeapObjectsMemoryPoolImpl;
import cn.itcraft.frogspawn.offheap.PrimitiveStructure;
import cn.itcraft.frogspawn.strategy.FetchFailStrategy;
import cn.itcraft.frogspawn.strategy.FetchStrategy;
import cn.itcraft.frogspawn.strategy.PoolStrategy;

/**
 * 对象内存池工厂类，用于创建不同类型的内存池实例
 * Objects memory pool factory class for creating different types of memory pool instances
 *
 * @author Helly Guo
 * <p>
 * Created on 8/24/21 11:31 PM
 */
public final class ObjectsMemoryPoolFactory {

    /**
     * 私有构造函数防止实例化
     * Private constructor to prevent instantiation
     */
    private ObjectsMemoryPoolFactory() {
    }

    /**
     * 创建新内存池（使用默认策略 FETCH_FAIL_AS_NEW/CALL_CREATOR）
     * Create new memory pool with default strategy (FETCH_FAIL_AS_NEW/CALL_CREATOR)
     *
     * @param creator 对象创建器 / Object creator
     * @param size    内存池大小 / Pool size
     * @param <T>     必须实现 Resettable 接口的类型 / Type must implement Resettable interface
     * @return 内存池实例 / Memory pool instance
     */
    public static <T extends Resettable> ObjectsMemoryPool<T> newPool(ObjectCreator<T> creator, int size) {
        return newPool(creator, size,
                       new PoolStrategy(FetchStrategy.FETCH_FAIL_AS_NEW, FetchFailStrategy.CALL_CREATOR));
    }

    /**
     * 创建新内存池（可指定策略）
     * Create new memory pool with specified strategy
     *
     * @param creator      对象创建器 / Object creator
     * @param size         内存池大小 / Pool size
     * @param poolStrategy 池策略 / Pool strategy
     * @param <T>          必须实现 Resettable 接口的类型 / Type must implement Resettable interface
     * @return 内存池实例 / Memory pool instance
     */
    public static <T extends Resettable> ObjectsMemoryPool<T> newPool(ObjectCreator<T> creator,
                                                                      int size,
                                                                      PoolStrategy poolStrategy) {
        if (poolStrategy == null) {
            throw new IllegalArgumentException("Pool strategy can not be null");
        }
        return new LocalObjectsMemoryPoolImpl<>(creator, size, poolStrategy);
    }

    /**
     * 创建非堆内存对象池
     * Create off-heap memory object pool
     *
     * @param creator 对象创建器 / Object creator
     * @param <T>     必须实现 PrimitiveStructure 接口的类型 / Type must implement PrimitiveStructure interface
     * @return 非堆内存池实例 / Off-heap memory pool instance
     */
    public static <T extends PrimitiveStructure> ObjectsMemoryPool<T> newOffHeapPool(ObjectCreator<T> creator) {
        return new OffHeapObjectsMemoryPoolImpl<>(creator);
    }

    /**
     * 创建固定非堆内存对象池
     * Create fixed off-heap memory object pool
     * 
     * 特点：
     * - 预创建所有对象，零创建开销
     * - 固定内存块，零内存分配开销
     * - 纯索引操作，最高性能
     * 
     * Features:
     * - Pre-create all objects, zero creation overhead
     * - Fixed memory block, zero allocation overhead
     * - Pure index operations, highest performance
     *
     * @param creator    对象创建器 / Object creator
     * @param capacity   池容量 / Pool capacity
     * @param objectSize 对象大小（字节） / Object size in bytes
     * @param <T>        必须实现 FixedOffHeapObject 接口的类型 / Type must implement FixedOffHeapObject interface
     * @return 固定非堆内存池实例 / Fixed off-heap memory pool instance
     */
    public static <T extends FixedOffHeapObject> ObjectsMemoryPool<T> newFixedOffHeapPool(
            ObjectCreator<T> creator, int capacity, int objectSize) {
        return new FixedOffHeapPool<>(creator, capacity, objectSize);
    }
}

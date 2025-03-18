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

import cn.itcraft.frogspawn.impl.ObjectsMemoryPoolImpl;
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

    // 私有构造函数防止实例化
    // Private constructor to prevent instantiation
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
                       new PoolStrategy(FetchStrategy.FETCH_FAIL_AS_NEW, FetchFailStrategy.CALL_CREATOR, false));
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
        return new ObjectsMemoryPoolImpl<>(creator, size, poolStrategy);
    }
}

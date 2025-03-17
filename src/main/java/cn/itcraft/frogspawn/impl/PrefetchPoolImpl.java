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
import cn.itcraft.frogspawn.strategy.FetchFailStrategy;
import cn.itcraft.frogspawn.strategy.PoolStrategy;

/**
 * 预取池实现类，用于管理和复用可重置对象
 * <p>
 * Prefetch pool implementation for managing and reusing resettable objects
 *
 * @param <T> 泛型类型，必须继承自 Resettable 接口
 *            Generic type that must extend Resettable interface
 * @author Helly Guo
 * <p>
 * Created on 8/24/21 11:34 PM
 */
public class PrefetchPoolImpl<T extends Resettable> extends AbstractPrefetchPoolImpl<T> {

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

    /**
     * 构造函数，初始化预取池
     * <p>
     * Constructor for initializing the prefetch pool
     *
     * @param creator      对象创建器实例
     *                     Object creator instance
     * @param size         预取池的初始容量
     *                     Initial capacity of the prefetch pool
     * @param poolStrategy 池策略，包含失败处理策略配置
     *                     Pool strategy containing failure handling configuration
     */
    public PrefetchPoolImpl(ObjectCreator<T> creator, int size, PoolStrategy poolStrategy) {
        super(creator, size);
        this.creator = creator;
        this.fetchFailStrategy = poolStrategy.getFetchFailStrategy();
    }

    /**
     * 从数据源获取数据的具体实现方法
     * 使用 FetchHelper 进行带故障转移机制的数据获取
     * <p>
     * Concrete implementation for fetching data from data source
     * Uses FetchHelper for data retrieval with failover mechanism
     *
     * @return 获取到的泛型对象实例 | Retrieved generic object instance
     */
    @Override
    protected T fetchData() {
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
                creator
                                              );
    }
}


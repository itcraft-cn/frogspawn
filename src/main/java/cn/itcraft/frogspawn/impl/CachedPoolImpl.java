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
 * 基于缓存的资源池实现类，支持对象复用和故障转移策略
 * Cached resource pool implementation class with object reuse and failover strategy support
 *
 * @author Helly Guo
 * <p>
 * 创建时间：2021年8月24日 23:34
 * Created on August 24, 2021 at 23:34
 */
public class CachedPoolImpl<T extends Resettable> extends AbstractCachedPool<T> {

    // 对象创建器，用于生成新的池对象
    // Object creator for generating new pool instances
    private final ObjectCreator<T> creator;

    // 资源获取失败时的处理策略
    // Strategy for handling resource acquisition failures
    private final FetchFailStrategy fetchFailStrategy;

    /**
     * 构造函数，初始化缓存池配置
     * Constructor for initializing cached pool configuration
     * 
     * @param creator      对象创建器实例
     *                    Object creator instance
     * @param size        资源池容量
     *                    Pool capacity
     * @param poolStrategy 资源池管理策略（包含获取失败策略）
     *                    Pool management strategy (includes fetch failure strategy)
     */
    public CachedPoolImpl(ObjectCreator<T> creator, int size, PoolStrategy poolStrategy) {
        super(creator, size);
        this.creator = creator;
        this.fetchFailStrategy = poolStrategy.getFetchFailStrategy();
    }

    /**
     * 从资源池获取数据对象的核心方法，实现故障转移机制
     * Core method for fetching data objects from pool with failover mechanism
     * 
     * @return 可复用的资源对象
     *         Reusable resource object
     */
    @Override
    protected T fetchData() {
        // 使用辅助类进行带故障转移的资源获取
        // Use helper class for resource fetching with failover
        return FetchHelper.fetchDataOrFailover(
            array,          // 对象存储数组 | Object storage array
            indexMask,      // 索引掩码用于快速定位 | Index mask for fast positioning
            walker,         // 索引遍历器 | Index walker
            fetchFailStrategy, // 失败处理策略 | Failure handling strategy
            creator         // 备用对象创建器 | Fallback object creator
        );
    }
}

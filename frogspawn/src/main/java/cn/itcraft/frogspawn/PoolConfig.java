/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.itcraft.frogspawn;

import cn.itcraft.frogspawn.strategy.FetchFailStrategy;
import cn.itcraft.frogspawn.strategy.FetchStrategy;
import cn.itcraft.frogspawn.strategy.PoolStrategy;

/**
 * 对象池配置
 * Object pool configuration
 *
 * 通过静态工厂方法选择池类型，配合统一创建入口：
 * Select the pool type through static factory methods and use the unified factory:
 *
 * <pre>{@code
 * ObjectsMemoryPool<MyPojo> pool = ObjectsMemoryPoolFactory.newPool(
 *         creator, PoolConfig.offHeap(1024));
 * }</pre>
 *
 * @author Helly Guo
 */
public final class PoolConfig {

    /**
     * 动态非堆池默认容量
     * Default capacity for the dynamic off-heap pool
     */
    private static final int DEFAULT_OFF_HEAP_CAPACITY = 1024;

    private final PoolType type;
    private final int capacity;
    private final int objectSize;
    private final PoolStrategy strategy;

    private PoolConfig(PoolType type, int capacity, int objectSize, PoolStrategy strategy) {
        if (type == null) {
            throw new IllegalArgumentException("Pool type can not be null");
        }
        if (capacity <= 0) {
            throw new IllegalArgumentException("Pool capacity must be positive");
        }
        if (type == PoolType.FIXED_OFF_HEAP && objectSize <= 0) {
            throw new IllegalArgumentException("Fixed off-heap object size must be positive");
        }
        if (type == PoolType.HEAP && strategy == null) {
            throw new IllegalArgumentException("Heap pool strategy can not be null");
        }
        this.type = type;
        this.capacity = capacity;
        this.objectSize = objectSize;
        this.strategy = strategy;
    }

    /**
     * 堆内存池配置（默认策略 FETCH_FAIL_AS_NEW / CALL_CREATOR）
     * Heap pool config with the default strategy
     *
     * @param capacity 池容量 / pool capacity
     * @return 配置 / config
     */
    public static PoolConfig heap(int capacity) {
        return heap(capacity,
                    new PoolStrategy(FetchStrategy.FETCH_FAIL_AS_NEW, FetchFailStrategy.CALL_CREATOR));
    }

    /**
     * 堆内存池配置
     * Heap pool config
     *
     * @param capacity 池容量 / pool capacity
     * @param strategy 池策略 / pool strategy
     * @return 配置 / config
     */
    public static PoolConfig heap(int capacity, PoolStrategy strategy) {
        return new PoolConfig(PoolType.HEAP, capacity, 0, strategy);
    }

    /**
     * 动态非堆池配置（默认容量 1024）
     * Dynamic off-heap pool config with the default capacity
     *
     * @return 配置 / config
     */
    public static PoolConfig offHeap() {
        return offHeap(DEFAULT_OFF_HEAP_CAPACITY);
    }

    /**
     * 动态非堆池配置
     * Dynamic off-heap pool config
     *
     * @param maxPoolSize 最大池容量 / max pool size
     * @return 配置 / config
     */
    public static PoolConfig offHeap(int maxPoolSize) {
        return new PoolConfig(PoolType.OFF_HEAP, maxPoolSize, 0, null);
    }

    /**
     * 固定非堆池配置
     * Fixed off-heap pool config
     *
     * @param capacity   池容量 / pool capacity
     * @param objectSize 对象大小（字节） / object size in bytes
     * @return 配置 / config
     */
    public static PoolConfig fixedOffHeap(int capacity, int objectSize) {
        return new PoolConfig(PoolType.FIXED_OFF_HEAP, capacity, objectSize, null);
    }

    public PoolType getType() {
        return type;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getObjectSize() {
        return objectSize;
    }

    public PoolStrategy getStrategy() {
        return strategy;
    }
}

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
package cn.itcraft.frogspawn.strategy;

import cn.itcraft.frogspawn.constants.Constants;

/**
 * 对象池策略枚举，定义从池中获取对象的不同策略
 * Object pool strategy enum, defines different strategies for fetching objects from the pool
 * 
 * @author Helly Guo
 * <p>
 * Created on 8/25/21 3:02 PM
 */
public enum PoolStrategy {
    /**
     * 循环获取，必须从池中取得对象（无限重试）
     * Keep retrying until successfully fetching an object from the pool (infinite retries)
     */
    MUST_FETCH_IN_POOL(-1, FetchFailStrategy.NOT_AVAILABLE),

    /**
     * 循环获取指定次数后，返回空值
     * Retry a certain number of times, return null if all attempts fail
     */
    FETCH_FAIL_AS_NULL(Constants.FETCH_TIMES, FetchFailStrategy.NULLABLE),

    /**
     * 循环获取指定次数后，创建新对象返回
     * Retry a certain number of times, create new instance if all attempts fail
     */
    FETCH_FAIL_AS_NEW(Constants.FETCH_TIMES, FetchFailStrategy.CALL_CREATOR);

    // 最大尝试次数（-1表示无限重试）
    // Maximum retry attempts (-1 means infinite retries)
    private final int fetchTimes;
    
    // 获取失败时的处理策略
    // Strategy when failed to fetch object from pool
    private final FetchFailStrategy fetchFailStrategy;

    /**
     * 构造函数
     * Constructor
     * 
     * @param fetchTimes 最大尝试次数 / Maximum retry attempts
     * @param fetchFailStrategy 失败处理策略 / Failure handling strategy
     */
    PoolStrategy(int fetchTimes, FetchFailStrategy fetchFailStrategy) {
        this.fetchTimes = fetchTimes;
        this.fetchFailStrategy = fetchFailStrategy;
    }

    /**
     * 获取最大尝试次数
     * Get maximum retry attempts
     * 
     * @return 重试次数（-1表示无限重试）
     *         Retry count (-1 indicates infinite retries)
     */
    public int getFetchTimes() {
        return fetchTimes;
    }

    /**
     * 获取失败处理策略
     * Get failure handling strategy
     * 
     * @return 失败策略枚举值
     *         Failure strategy enum value
     */
    public FetchFailStrategy getFetchFailStrategy() {
        return fetchFailStrategy;
    }
}

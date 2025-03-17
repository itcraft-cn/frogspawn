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

/**
 * 对象池策略，定义从池中获取对象的不同策略
 * Object pool strategy enum, defines different strategies for fetching objects from the pool
 *
 * @author Helly Guo
 * <p>
 * Created on 8/25/21 3:02 PM
 */
public class PoolStrategy {
    private final FetchFailStrategy fetchFailStrategy;
    private final boolean prefetch;

    public PoolStrategy(FetchFailStrategy fetchFailStrategy, boolean prefetch) {
        this.fetchFailStrategy = fetchFailStrategy;
        this.prefetch = prefetch;
    }

    public FetchFailStrategy getFetchFailStrategy() {
        return fetchFailStrategy;
    }

    public boolean isPrefetch() {
        return prefetch;
    }
}

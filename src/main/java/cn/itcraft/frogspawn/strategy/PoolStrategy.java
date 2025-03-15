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
 * @author Helly Guo
 * <p>
 * Created on 8/25/21 3:02 PM
 */
public enum PoolStrategy {
    /**
     * 循环，必定取到池中的对象
     */
    MUST_FETCH_IN_POOL(-1, FetchFailStrategy.NOT_AVAILABLE),
    /**
     * 循环一定次数后，失败返回空
     */
    FETCH_FAIL_AS_NULL(Constants.FETCH_TIMES, FetchFailStrategy.NULLABLE),
    /**
     * 循环一定次数后，失败返回新创建对象
     */
    FETCH_FAIL_AS_NEW(Constants.FETCH_TIMES, FetchFailStrategy.CALL_CREATOR);

    private final int fetchTimes;
    private final FetchFailStrategy fetchFailStrategy;

    PoolStrategy(int fetchTimes, FetchFailStrategy fetchFailStrategy) {
        this.fetchTimes = fetchTimes;
        this.fetchFailStrategy = fetchFailStrategy;
    }

    public int getFetchTimes() {
        return fetchTimes;
    }

    public FetchFailStrategy getFetchFailStrategy() {
        return fetchFailStrategy;
    }
}

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
package cn.itcraft.frogspawn.constants;

import cn.itcraft.frogspawn.util.ArrayUtil;

/**
 * @author Helly Guo
 * <p>
 * Created on 8/25/21 3:08 PM
 */
public final class Constants {

    /**
     * 最大循环取次数，默认 10
     */
    private static final String DEFAULT_FETCH_TIMES = "10";
    /**
     * 实际运行中最大循环取次数
     */
    public static final int FETCH_TIMES
            = Integer.parseInt(System.getProperty("frogspawn.fetch.times", DEFAULT_FETCH_TIMES));
    /**
     * 最大容量，默认 65536*16
     */
    private static final String DEFAULT_MAX_CAPACITY = "1048576";
    /**
     * 实际运行中最大容量
     */
    public static final int MAX_CAPACITY
            = Integer.parseInt(System.getProperty("frogspawn.max.capacity", DEFAULT_MAX_CAPACITY));
    /**
     * 默认缓存大小，默认 8，最大 64
     * Default cache capacity, default 8, maximum 64
     * <p>
     * 当系统属性未配置时将使用此默认值
     * This default value will be used when system property is not configured
     */
    private static final String DEFAULT_CACHE_CAPACITY = "8";

    /**
     * 最大缓存大小限制，64
     * Maximum cache capacity limit, 64
     * <p>
     * 用于确保缓存容量不超过安全阈值
     * Used to ensure cache capacity does not exceed safe threshold
     */
    private static final int MAX_CACHE_CAPACITY = 64;

    /**
     * 实际使用的缓存容量值
     * Actual cache capacity value used
     * <p>
     * 通过以下步骤计算得出：
     * Calculated through following steps:
     * 1. 读取系统属性 "frogspawn.cache.capacity"，未配置时使用默认值
     *    Read system property "frogspawn.cache.capacity", use default if not configured
     * 2. 转换为最接近的2的幂次方（适配哈希表等数据结构需求）
     *    Convert to nearest positive power of two (for hash table optimization)
     * 3. 确保最终值不超过最大限制值
     *    Ensure final value does not exceed maximum limit
     */
    public static final int CACHE_CAPACITY = Math.min(
            ArrayUtil.findNextPositivePowerOfTwo(
                    Integer.parseInt(System.getProperty("frogspawn.cache.capacity", DEFAULT_CACHE_CAPACITY))),
            MAX_CACHE_CAPACITY);

    /**
     * 私有构造器防止类实例化
     * Private constructor to prevent class instantiation
     * <p>
     * 工具类/常量类的最佳实践
     * Best practice for utility/constant classes
     */
    private Constants() {
    }
}

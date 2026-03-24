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
package cn.itcraft.frogspawn.misc;

import cn.itcraft.frogspawn.Resettable;
import cn.itcraft.frogspawn.util.ArrayUtil;

import static cn.itcraft.frogspawn.constants.Constants.CACHE_CAPACITY;

/**
 * 基于栈结构的可重置对象缓存容器
 * A stack-based cache storage container for resettable objects
 * <p>
 * 相比 SoftRefStore 的优势：
 * - O(1) 时间复杂度的 fetch/release 操作
 * - 无 SoftReference 开销
 * - 无 walker 指针维护
 * <p>
 * Advantages over SoftRefStore:
 * - O(1) time complexity for fetch/release operations
 * - No SoftReference overhead
 * - No walker pointer maintenance
 *
 * @author Helly Guo
 * <p>
 * Created on 2026-03-24
 */
public class SimpleStackCache<T extends Resettable> {

    private static final int CAPACITY = CACHE_CAPACITY;

    private final Object[] cache;
    private int size = 0;

    public SimpleStackCache() {
        this.cache = ArrayUtil.createArray(Object.class, CAPACITY);
    }

    /**
     * 从缓存中获取一个可用对象
     * Fetch an available object from the cache
     *
     * @return 可用对象或null（缓存为空时）
     * Available object or null (when cache is empty)
     */
    @SuppressWarnings("unchecked")
    public T fetch() {
        if (size == 0) {
            return null;
        }
        size--;
        T obj = (T) cache[size];
        cache[size] = null;
        return obj;
    }

    /**
     * 将对象释放回缓存
     * Release object back to cache
     *
     * @param obj 需要释放的对象 object to release
     * @return true-需要继续释放流程 false-不需要继续
     * true-need to continue release process false-no need
     */
    public boolean release(T obj) {
        if (obj == null || size >= CAPACITY) {
            return true;
        }
        cache[size] = obj;
        size++;
        return false;
    }

    /**
     * 检查缓存是否为空
     * Check if cache is empty
     *
     * @return 是否为空 whether empty
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * 获取当前缓存大小
     * Get current cache size
     *
     * @return 缓存大小 cache size
     */
    public int size() {
        return size;
    }
}
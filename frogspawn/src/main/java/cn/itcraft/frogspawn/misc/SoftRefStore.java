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

import java.lang.ref.SoftReference;

import static cn.itcraft.frogspawn.constants.Constants.CACHE_CAPACITY;
import static cn.itcraft.frogspawn.util.ArrayUtil.elementAt;
import static cn.itcraft.frogspawn.util.ArrayUtil.fillElementAt;

/**
 * 基于软引用实现的可重置对象缓存存储容器
 * A soft-reference based cache storage container for resettable objects
 *
 * @author Helly Guo
 * <p>
 * Created on 8/29/21 3:45 PM
 */
public class SoftRefStore<T extends Resettable> {
    // 初始缓存容量，应该配置为2的幂次方
    // Initial cache capacity, should be configured as a power of two
    private static final int INITIAL_CAPACITY = CACHE_CAPACITY;

    // 位掩码用于快速取模运算
    // Bit mask for fast modulo operation
    private static final int MASK = INITIAL_CAPACITY - 1;

    // 使用软引用持有对象数组，允许在内存不足时被GC回收
    // Soft reference holding object array, allows GC to reclaim when memory is low
    private final SoftReference<Object[]> softRef =
            new SoftReference<>(ArrayUtil.createArray(Object.class, INITIAL_CAPACITY));

    // 当前缓存中的对象数量
    // Current number of objects in the cache
    private int size = 0;

    // 获取对象的遍历指针（用于轮询算法）
    // Fetch pointer for round-robin algorithm
    private long fetchWalker = 0L;

    // 释放对象的遍历指针（用于轮询算法）
    // Release pointer for round-robin algorithm
    private long releaseWalker = 0L;

    /**
     * 从缓存中获取一个可用对象
     * Fetch an available object from the cache
     *
     * @return 可用对象或null（缓存为空时）
     * Available object or null (when cache is empty)
     */
    public T fetch() {
        if (size == 0) {
            return null;
        }
        Object[] array = softRef.get();
        return array == null ? null : getFromArray(array);
    }

    /**
     * 遍历数组查找可用对象
     * Traverse array to find available object
     *
     * @param array 目标数组 target array
     * @return 找到的对象或null not found object or null
     */
    private T getFromArray(Object[] array) {
        T t;
        for (int i = 0; i < INITIAL_CAPACITY; i++) {
            t = fetchArrayElement(array, i);
            if (t != null) {
                return t;
            }
        }
        return null;
    }

    /**
     * 获取数组指定偏移位置的元素
     * Get element at specified offset position
     *
     * @param array 目标数组 target array
     * @param idx   相对偏移量 relative offset
     * @return 获取到的对象或null retrieved object or null
     */
    @SuppressWarnings("unchecked")
    private T fetchArrayElement(Object[] array, int idx) {
        long newVal = fetchWalker + idx + 1;
        T obj = (T) elementAt(array, MASK, newVal);
        if (obj == null) {
            return null;
        }
        fillElementAt(array, MASK, newVal, null);
        fetchWalker = newVal;
        size--;
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
        if (obj == null || size == INITIAL_CAPACITY) {
            return true;
        }
        Object[] array = softRef.get();
        return array == null || putToArray(array, obj);
    }

    /**
     * 尝试将对象放入数组
     * Try to put the object into the array
     *
     * @param array 目标数组 target array
     * @param obj   需要存储的对象 object to store
     * @return 是否成功放入（false表示成功）
     * whether successfully stored (false means success)
     */
    private boolean putToArray(Object[] array, T obj) {
        for (int i = 0; i < INITIAL_CAPACITY; i++) {
            if (restoreArrayElement(array, obj, i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 尝试在指定偏移位置恢复元素
     * Try to restore the element at specified offset
     *
     * @param array 目标数组 target array
     * @param obj   需要存储的对象 object to store
     * @param idx   相对偏移量 relative offset
     * @return 是否成功存储 whether successfully stored
     */
    @SuppressWarnings("unchecked")
    private boolean restoreArrayElement(Object[] array, T obj, int idx) {
        long newVal = releaseWalker + idx + 1;
        T x = (T) elementAt(array, MASK, newVal);
        if (x == null) {
            fillElementAt(array, MASK, newVal, obj);
            releaseWalker = newVal;
            size++;
            return true;
        }
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
}

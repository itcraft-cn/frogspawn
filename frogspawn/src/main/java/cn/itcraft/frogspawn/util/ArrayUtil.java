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
package cn.itcraft.frogspawn.util;

import cn.itcraft.frogspawn.platform.MemoryAccess;
import cn.itcraft.frogspawn.platform.PlatformMemory;

import java.lang.reflect.Array;

/**
 * 数组操作工具类
 * Array manipulation utility class
 * <p>
 * 平台相关访问通过 {@link PlatformMemory} 抽离：
 * JDK8 使用 Unsafe，JDK9+ 使用 VarHandle。
 * Platform-specific access is isolated behind {@link PlatformMemory}:
 * JDK8 uses Unsafe, JDK9+ uses VarHandle.
 *
 * @author Helly Guo
 * <p>
 * Created on 12/9/21 7:11 PM
 */
public final class ArrayUtil {

    /**
     * 补全长度
     * Buffer padding length
     */
    public static final int BUFFER_PAD;
    /**
     * 元素引用转换值
     * Element reference shift
     */
    public static final int REF_ELEMENT_SHIFT;

    private static final MemoryAccess MEMORY = PlatformMemory.ACCESS;
    private static final int CACHE_LINE_BYTES = 128;

    static {
        int scale = MEMORY.pointerSize();
        if (scale != 4 && scale != 8) {
            throw new IllegalStateException("Unknown pointer size");
        }
        REF_ELEMENT_SHIFT = Integer.numberOfTrailingZeros(scale);
        BUFFER_PAD = CACHE_LINE_BYTES / scale;
    }

    /**
     * 私有构造函数 - 工具类禁止实例化
     * Private constructor - utility class should not be instantiated
     */
    private ArrayUtil() {
    }

    /**
     * 创建带缓冲填充的数组（用于防止伪共享）
     * Create array with buffer padding (to prevent false sharing)
     *
     * @param clazz    数组元素类型 Array element type
     * @param capacity 实际容量 Actual capacity
     * @param <T>      元素类型 Element type
     * @return 带前后填充的数组 Array with front and rear padding
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] createArray(Class<T> clazz, int capacity) {
        return (T[]) Array.newInstance(clazz, capacity + 2 * BUFFER_PAD);
    }

    /**
     * 读取数组中指定位置的元素
     * Get an element at specified position in the array
     *
     * @param array    目标数组 Target array
     * @param mask     掩码（用于快速取模）Mask for fast modulo
     * @param sequence 元素序号 Element sequence
     * @param <T>      元素类型 Element type
     * @return 数组元素 Array element
     */
    @SuppressWarnings("unchecked")
    public static <T> T elementAt(T[] array, int mask, long sequence) {
        int index = (int) (sequence & mask) + BUFFER_PAD;
        return (T) MEMORY.getArrayElement(array, index);
    }

    /**
     * 向数组指定位置写入元素
     * Set element at specified position in array
     *
     * @param array    目标数组 Target array
     * @param mask     掩码（用于快速取模）Mask for fast modulo
     * @param sequence 元素序号 Element sequence
     * @param t        要写入的值 Value to set
     * @param <T>      元素类型 Element type
     */
    public static <T> void fillElementAt(T[] array, int mask, long sequence, T t) {
        int index = (int) (sequence & mask) + BUFFER_PAD;
        MEMORY.setArrayElement(array, index, t);
    }

    /**
     * 查找不小于输入值的最小2的幂次方。当输入为0时返回1。若输入为负数则抛出异常。
     * Finds the smallest power of two that is greater than or equal to the input value. Returns 1 if the input is 0. Throws an exception for negative inputs.
     * <p>
     * 实现原理：计算(value - 1)的二进制前导零的数量，32减去该数得到需要左移的位数。由于1左移该位数得到的结果即为目标值。
     * Implementation: Calculates the number of leading zeros in (value - 1)'s binary form. Subtracting this from 32 gives the shift amount. Left-shifting 1 by this amount yields the desired power of two.
     *
     * @param value 输入的正整数，必须非负。The input value, must be non-negative.
     * @return 不小于输入值的最小2的幂次方。The smallest power of two &gt; the input value.
     * @throws IllegalArgumentException 当输入值为负数时抛出。Thrown if the input value is negative.
     */
    public static int findNextPositivePowerOfTwo(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("must be positive");
        }
        // 通过位运算快速计算最小2的幂
        // Efficient bitwise calculation for the smallest power of two
        return 1 << (32 - Integer.numberOfLeadingZeros(value - 1));
    }
}

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
package cn.itcraft.frogspawn.util;

import sun.misc.Unsafe;

import java.lang.reflect.Array;

/**
 * 数组操作工具类
 * Array manipulation utility class
 * <p>
 * 提供高性能的数组操作，使用Unsafe类实现底层内存操作
 * Provides high-performance array operations using Unsafe class for low-level memory manipulation
 *
 * @author Helly Guo
 * <p>
 * Created on 12/9/21 7:11 PM
 */
public final class ArrayUtil {

    /**
     * 补全长度
     */
    public static final int BUFFER_PAD;
    /**
     * 数组下标引用基础
     */
    public static final long REF_ARRAY_BASE;
    /**
     * 元素引用转换值
     */
    public static final int REF_ELEMENT_SHIFT;
    private static final Unsafe UNSAFE = UnsafeUtil.getUnsafe();

    // 静态初始化块 - 初始化内存布局相关常量
    // Static initializer - Initialize memory layout related constants
    static {
        // 获取Object数组每个元素占用的字节数（指针大小）
        // Get the size in bytes of each element in Object array (pointer size)
        int scale = UNSAFE.arrayIndexScale(Object[].class);

        // 根据指针大小确定元素偏移量位移数
        // Determine element offset shift based on pointer size
        if (4 == scale) {
            // 32位系统 32-bit system
            // 2^2 = 4 bytes per element
            REF_ELEMENT_SHIFT = 2;
        } else if (8 == scale) {
            // 64位系统 64-bit system
            // 2^3 = 8 bytes per element
            REF_ELEMENT_SHIFT = 3;
        } else {
            throw new IllegalStateException("Unknown pointer size");
        }

        // 计算缓存行填充元素个数（假设缓存行大小为128字节）
        // Calculate buffer padding elements (assuming 128-byte cache line)
        BUFFER_PAD = 128 / scale;

        // 计算数组基准地址（包含前置填充）
        // Calculate array base offset (including front buffer padding)
        REF_ARRAY_BASE = UNSAFE.arrayBaseOffset(Object[].class) + (BUFFER_PAD << REF_ELEMENT_SHIFT);
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
     * 计算元素在数组中的内存偏移量
     * Calculate memory offset of the element in array
     *
     * @param sequence 元素序号 Element sequence number
     * @param mask     掩码（通常为容量-1）Mask (usually capacity-1)
     * @return 内存偏移量 Memory offset
     */
    private static long locateInArray(long sequence, int mask) {
        return REF_ARRAY_BASE + ((sequence & mask) << REF_ELEMENT_SHIFT);
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
        return (T) UNSAFE.getObject(array, locateInArray(sequence, mask));
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
        UNSAFE.putObject(array, locateInArray(sequence, mask), t);
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
     * @throws RuntimeException 当输入值为负数时抛出。Thrown if the input value is negative.
     */
    public static int findNextPositivePowerOfTwo(int value) {
        if (value < 0) {
            throw new RuntimeException("must be positive");
        }
        // 通过位运算快速计算最小2的幂
        // Efficient bitwise calculation for the smallest power of two
        return 1 << (32 - Integer.numberOfLeadingZeros(value - 1));
    }
}

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

    static {
        int scale = UNSAFE.arrayIndexScale(Object[].class);
        if (4 == scale) {
            REF_ELEMENT_SHIFT = 2;
        } else if (8 == scale) {
            REF_ELEMENT_SHIFT = 3;
        } else {
            throw new IllegalStateException("Unknown pointer size");
        }
        BUFFER_PAD = 128 / scale;
        // Including the buffer pad in the array base offset
        REF_ARRAY_BASE = UNSAFE.arrayBaseOffset(Object[].class) + (BUFFER_PAD << REF_ELEMENT_SHIFT);
    }

    private ArrayUtil() {
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] createArray(Class<T> clazz, int capacity) {
        return (T[]) Array.newInstance(clazz, capacity + 2 * BUFFER_PAD);
    }

    private static long locateInArray(long sequence, int mask) {
        return REF_ARRAY_BASE + ((sequence & mask) << REF_ELEMENT_SHIFT);
    }

    /**
     * 读取被包装元素
     *
     * @param sequence id
     * @return obj
     */
    @SuppressWarnings("unchecked")
    public static <T> T elementAt(T[] array, int mask, long sequence) {
        return (T) UNSAFE.getObject(array, locateInArray(sequence, mask));
    }

    /**
     * 写入被包装元素
     *
     * @param sequence id
     */
    public static <T> void fillElementAt(T[] array, int mask, long sequence, T t) {
        UNSAFE.putObject(array, locateInArray(sequence, mask), t);
    }

    /**
     * 查找比输入略大的2的幂次方
     *
     * @param value 传入的值
     * @return power of two
     */
    public static int findNextPositivePowerOfTwo(int value) {
        if (value < 0) {
            throw new RuntimeException("must be positive");
        }
        return 1 << (32 - Integer.numberOfLeadingZeros(value - 1));
    }
}

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
package cn.itcraft.frogspawn.platform;

/**
 * 平台内存访问 SPI
 * Platform memory access SPI
 *
 * 由 JDK 专有实现提供：
 * - JDK8: sun.misc.Unsafe
 * - JDK9-23: VarHandle 数组访问 + RawMemoryUtil
 * - JDK24+: java.lang.foreign.MemorySegment
 * <p>
 * Implemented per JDK:
 * - JDK8: sun.misc.Unsafe
 * - JDK9-23: VarHandle array access + RawMemoryUtil
 * - JDK24+: java.lang.foreign.MemorySegment
 *
 * @author Helly Guo
 */
@SuppressWarnings("PMD.TooManyMethods")
public interface MemoryAccess {

    /**
     * 指针大小（字节），4 或 8
     * Pointer size in bytes, 4 or 8
     *
     * @return 指针大小 / pointer size
     */
    int pointerSize();

    /**
     * 分配原生内存
     * Allocate native memory
     *
     * @param bytes 字节数 / byte count
     * @return 内存地址 / memory address
     */
    long allocateMemory(long bytes);

    /**
     * 释放原生内存
     * Free native memory
     *
     * @param address 内存地址 / memory address
     */
    void freeMemory(long address);

    /**
     * 读取对象数组指定下标元素
     * Read an object array element at the given index
     *
     * @param array 目标数组 / target array
     * @param index 下标 / index
     * @return 元素 / element
     */
    Object getArrayElement(Object[] array, int index);

    /**
     * 写入对象数组指定下标元素
     * Write an object array element at the given index
     *
     * @param array 目标数组 / target array
     * @param index 下标 / index
     * @param value 元素值 / element value
     */
    void setArrayElement(Object[] array, int index, Object value);

    byte getByte(long address);

    void putByte(long address, byte value);

    short getShort(long address);

    void putShort(long address, short value);

    char getChar(long address);

    void putChar(long address, char value);

    int getInt(long address);

    void putInt(long address, int value);

    long getLong(long address);

    void putLong(long address, long value);

    float getFloat(long address);

    void putFloat(long address, float value);

    double getDouble(long address);

    void putDouble(long address, double value);

    /**
     * 将指定范围内存填充为指定字节
     * Fill the given memory range with a byte value
     *
     * @param address 起始地址 / start address
     * @param bytes   字节数 / byte count
     * @param value   填充值 / fill value
     */
    void setMemory(long address, long bytes, byte value);

    /**
     * 拷贝原生内存
     * Copy native memory
     *
     * @param srcAddress 源地址 / source address
     * @param dstAddress 目标地址 / destination address
     * @param bytes      字节数 / byte count
     */
    void copyMemory(long srcAddress, long dstAddress, long bytes);
}

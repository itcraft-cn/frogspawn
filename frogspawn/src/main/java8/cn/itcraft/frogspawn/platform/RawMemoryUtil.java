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

import cn.itcraft.frogspawn.util.UnsafeUtil;
import sun.misc.Unsafe;

/**
 * JDK8-23 原生内存访问实现
 * JDK8-23 raw memory access implementation
 *
 * JDK24+ 请使用基于 MemorySegment 的覆盖实现。
 * For JDK24+ use the MemorySegment based overlay.
 *
 * @author Helly Guo
 */
final class RawMemoryUtil {

    private static final Unsafe UNSAFE = UnsafeUtil.getUnsafe();

    private RawMemoryUtil() {
    }

    static long allocate(long bytes) {
        return UNSAFE.allocateMemory(bytes);
    }

    static void free(long address) {
        UNSAFE.freeMemory(address);
    }

    static byte getByte(long address) {
        return UNSAFE.getByte(address);
    }

    static void putByte(long address, byte value) {
        UNSAFE.putByte(address, value);
    }

    static short getShort(long address) {
        return UNSAFE.getShort(address);
    }

    static void putShort(long address, short value) {
        UNSAFE.putShort(address, value);
    }

    static char getChar(long address) {
        return UNSAFE.getChar(address);
    }

    static void putChar(long address, char value) {
        UNSAFE.putChar(address, value);
    }

    static int getInt(long address) {
        return UNSAFE.getInt(address);
    }

    static void putInt(long address, int value) {
        UNSAFE.putInt(address, value);
    }

    static long getLong(long address) {
        return UNSAFE.getLong(address);
    }

    static void putLong(long address, long value) {
        UNSAFE.putLong(address, value);
    }

    static float getFloat(long address) {
        return UNSAFE.getFloat(address);
    }

    static void putFloat(long address, float value) {
        UNSAFE.putFloat(address, value);
    }

    static double getDouble(long address) {
        return UNSAFE.getDouble(address);
    }

    static void putDouble(long address, double value) {
        UNSAFE.putDouble(address, value);
    }

    static void setMemory(long address, long bytes, byte value) {
        UNSAFE.setMemory(address, bytes, value);
    }

    static void copyMemory(long srcAddress, long dstAddress, long bytes) {
        UNSAFE.copyMemory(srcAddress, dstAddress, bytes);
    }
}

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

import java.nio.ByteBuffer;

/**
 * JDK8-23 原生内存访问基类
 * JDK8-23 raw memory access base class
 *
 * 复用 RawMemoryUtil，供 java8 与 java9 覆盖实现继承，避免重复代码。
 * Reuses RawMemoryUtil and is extended by the java8 and java9 overlays to
 * avoid duplicated code.
 *
 * @author Helly Guo
 */
@SuppressWarnings("PMD.TooManyMethods")
abstract class AbstractRawMemoryAccess implements MemoryAccess {

    @Override
    public long allocateMemory(long bytes) {
        return RawMemoryUtil.allocate(bytes);
    }

    @Override
    public void freeMemory(long address) {
        RawMemoryUtil.free(address);
    }

    @Override
    public byte getByte(long address) {
        return RawMemoryUtil.getByte(address);
    }

    @Override
    public void putByte(long address, byte value) {
        RawMemoryUtil.putByte(address, value);
    }

    @Override
    public short getShort(long address) {
        return RawMemoryUtil.getShort(address);
    }

    @Override
    public void putShort(long address, short value) {
        RawMemoryUtil.putShort(address, value);
    }

    @Override
    public char getChar(long address) {
        return RawMemoryUtil.getChar(address);
    }

    @Override
    public void putChar(long address, char value) {
        RawMemoryUtil.putChar(address, value);
    }

    @Override
    public int getInt(long address) {
        return RawMemoryUtil.getInt(address);
    }

    @Override
    public void putInt(long address, int value) {
        RawMemoryUtil.putInt(address, value);
    }

    @Override
    public long getLong(long address) {
        return RawMemoryUtil.getLong(address);
    }

    @Override
    public void putLong(long address, long value) {
        RawMemoryUtil.putLong(address, value);
    }

    @Override
    public float getFloat(long address) {
        return RawMemoryUtil.getFloat(address);
    }

    @Override
    public void putFloat(long address, float value) {
        RawMemoryUtil.putFloat(address, value);
    }

    @Override
    public double getDouble(long address) {
        return RawMemoryUtil.getDouble(address);
    }

    @Override
    public void putDouble(long address, double value) {
        RawMemoryUtil.putDouble(address, value);
    }

    @Override
    public void setMemory(long address, long bytes, byte value) {
        RawMemoryUtil.setMemory(address, bytes, value);
    }

    @Override
    public void copyMemory(long srcAddress, long dstAddress, long bytes) {
        RawMemoryUtil.copyMemory(srcAddress, dstAddress, bytes);
    }

    @Override
    public ByteBuffer asByteBuffer(long address, long size) {
        throw new UnsupportedOperationException(
                "byte buffer view requires JDK24+ or a direct ByteBuffer-backed object");
    }
}

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

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JDK24+ 平台内存访问实现
 * JDK24+ platform memory access implementation
 *
 * 完全不使用 sun.misc.Unsafe：
 * - 原生内存通过 java.lang.foreign.MemorySegment 分配/释放
 * - 数组访问使用 VarHandle
 * - 内存拷贝使用 MemorySegment.copyFrom
 * <p>
 * Uses no sun.misc.Unsafe:
 * - native memory is managed by java.lang.foreign.MemorySegment
 * - array access uses VarHandle
 * - memory copy uses MemorySegment.copyFrom
 *
 * @author Helly Guo
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.LawOfDemeter"})
public final class MemoryAccessImpl implements MemoryAccess {

    private static final int POINTER_SIZE = 8;
    private static final VarHandle OBJECT_ELEMENTS =
            MethodHandles.arrayElementVarHandle(Object[].class);
    private static final ConcurrentHashMap<Long, Allocation> ALLOCATIONS =
            new ConcurrentHashMap<>(16);

    private static final class Allocation {
        private final Arena arena;
        private final MemorySegment segment;

        private Allocation(Arena arena, MemorySegment segment) {
            this.arena = arena;
            this.segment = segment;
        }

        private long address() {
            return segment.address();
        }

        private void close() {
            arena.close();
        }
    }

    @Override
    public int pointerSize() {
        return POINTER_SIZE;
    }

    @Override
    @SuppressWarnings("PMD.CloseResource")
    public long allocateMemory(long bytes) {
        Arena arena = Arena.ofShared();
        MemorySegment segment = arena.allocate(bytes);
        Allocation allocation = new Allocation(arena, segment);
        long address = allocation.address();
        ALLOCATIONS.put(address, allocation);
        return address;
    }

    @Override
    public void freeMemory(long address) {
        Allocation allocation = ALLOCATIONS.remove(address);
        if (allocation != null) {
            allocation.close();
        }
    }

    @Override
    public Object getArrayElement(Object[] array, int index) {
        return OBJECT_ELEMENTS.get(array, index);
    }

    @Override
    public void setArrayElement(Object[] array, int index, Object value) {
        OBJECT_ELEMENTS.set(array, index, value);
    }

    @Override
    public byte getByte(long address) {
        return segment(address, Byte.BYTES).get(ValueLayout.JAVA_BYTE, 0);
    }

    @Override
    public void putByte(long address, byte value) {
        segment(address, Byte.BYTES).set(ValueLayout.JAVA_BYTE, 0, value);
    }

    @Override
    public short getShort(long address) {
        return segment(address, Short.BYTES).get(ValueLayout.JAVA_SHORT, 0);
    }

    @Override
    public void putShort(long address, short value) {
        segment(address, Short.BYTES).set(ValueLayout.JAVA_SHORT, 0, value);
    }

    @Override
    public char getChar(long address) {
        return segment(address, Character.BYTES).get(ValueLayout.JAVA_CHAR, 0);
    }

    @Override
    public void putChar(long address, char value) {
        segment(address, Character.BYTES).set(ValueLayout.JAVA_CHAR, 0, value);
    }

    @Override
    public int getInt(long address) {
        return segment(address, Integer.BYTES).get(ValueLayout.JAVA_INT, 0);
    }

    @Override
    public void putInt(long address, int value) {
        segment(address, Integer.BYTES).set(ValueLayout.JAVA_INT, 0, value);
    }

    @Override
    public long getLong(long address) {
        return segment(address, Long.BYTES).get(ValueLayout.JAVA_LONG, 0);
    }

    @Override
    public void putLong(long address, long value) {
        segment(address, Long.BYTES).set(ValueLayout.JAVA_LONG, 0, value);
    }

    @Override
    public float getFloat(long address) {
        return segment(address, Float.BYTES).get(ValueLayout.JAVA_FLOAT, 0);
    }

    @Override
    public void putFloat(long address, float value) {
        segment(address, Float.BYTES).set(ValueLayout.JAVA_FLOAT, 0, value);
    }

    @Override
    public double getDouble(long address) {
        return segment(address, Double.BYTES).get(ValueLayout.JAVA_DOUBLE, 0);
    }

    @Override
    public void putDouble(long address, double value) {
        segment(address, Double.BYTES).set(ValueLayout.JAVA_DOUBLE, 0, value);
    }

    @Override
    public void setMemory(long address, long bytes, byte value) {
        segment(address, bytes).fill(value);
    }

    @Override
    public void copyMemory(long srcAddress, long dstAddress, long bytes) {
        MemorySegment source = segment(srcAddress, bytes);
        MemorySegment target = segment(dstAddress, bytes);
        target.copyFrom(source);
    }

    @Override
    public ByteBuffer asByteBuffer(long address, long size) {
        return segment(address, size).asByteBuffer();
    }

    private static MemorySegment segment(long address, long bytes) {
        return MemorySegment.ofAddress(address).reinterpret(bytes);
    }
}

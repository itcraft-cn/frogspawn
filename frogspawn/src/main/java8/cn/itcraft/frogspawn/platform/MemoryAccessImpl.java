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
 * JDK8 平台内存访问实现
 * JDK8 platform memory access implementation
 *
 * 全部通过 sun.misc.Unsafe 完成，数组访问使用原始偏移量。
 * All operations use sun.misc.Unsafe; array access uses raw offsets.
 *
 * @author Helly Guo
 */
public final class MemoryAccessImpl implements MemoryAccess {

    private static final Unsafe UNSAFE = UnsafeUtil.getUnsafe();
    private static final int POINTER_SIZE = UNSAFE.arrayIndexScale(Object[].class);
    private static final int ELEMENT_SHIFT = Integer.numberOfTrailingZeros(POINTER_SIZE);
    private static final long ARRAY_BASE = UNSAFE.arrayBaseOffset(Object[].class);

    @Override
    public int pointerSize() {
        return POINTER_SIZE;
    }

    @Override
    public long allocateMemory(long bytes) {
        return RawMemoryUtil.allocate(bytes);
    }

    @Override
    public void freeMemory(long address) {
        RawMemoryUtil.free(address);
    }

    @Override
    public Object getArrayElement(Object[] array, int index) {
        return UNSAFE.getObject(array, ARRAY_BASE + ((long) index << ELEMENT_SHIFT));
    }

    @Override
    public void setArrayElement(Object[] array, int index, Object value) {
        UNSAFE.putObject(array, ARRAY_BASE + ((long) index << ELEMENT_SHIFT), value);
    }
}

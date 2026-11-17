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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * JDK9+ 平台内存访问实现
 * JDK9+ platform memory access implementation
 *
 * 数组访问使用 VarHandle；原生内存分配/释放复用 JDK8 的 RawMemoryUtil。
 * JDK9-21 没有公开的原始地址分配 API，后续可增加 java22 overlay
 * 切换到 java.lang.foreign。
 * <p>
 * Array access uses VarHandle; raw memory allocation/free reuses the JDK8
 * RawMemoryUtil implementation. JDK9-21 has no public raw-address allocation API;
 * a future java22 overlay can switch to java.lang.foreign.
 *
 * @author Helly Guo
 */
public final class MemoryAccessImpl implements MemoryAccess {

    private static final VarHandle OBJECT_ELEMENTS =
            MethodHandles.arrayElementVarHandle(Object[].class);
    private static final int POINTER_SIZE =
            "32".equals(System.getProperty("sun.arch.data.model")) ? 4 : 8;

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
        return OBJECT_ELEMENTS.get(array, index);
    }

    @Override
    public void setArrayElement(Object[] array, int index, Object value) {
        OBJECT_ELEMENTS.set(array, index, value);
    }

}

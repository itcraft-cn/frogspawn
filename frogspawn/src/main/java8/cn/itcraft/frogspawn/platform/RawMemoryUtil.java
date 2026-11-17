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
 * JDK8 原生内存分配器
 * JDK8 raw memory allocator
 *
 * JDK9-21 没有公开的原始地址分配 API，因此 JDK9+ 覆盖层复用本类；
 * 后续可用 java.lang.foreign 的 java22 overlay 替换。
 * JDK9-21 has no public raw-address allocation API, so the JDK9+ overlay
 * reuses this class; a future java22 overlay can switch to java.lang.foreign.
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
}

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
 * 平台内存访问入口
 * Platform memory access entry point
 *
 * 通过反射加载同包下的 MemoryAccessImpl：
 * - MR-JAR base 层提供 JDK8 实现
 * - META-INF/versions/9 提供 JDK9+ 实现
 * <p>
 * Loads the same-package MemoryAccessImpl reflectively:
 * - the MR-JAR base layer provides the JDK8 implementation
 * - META-INF/versions/9 provides the JDK9+ implementation
 *
 * @author Helly Guo
 */
public final class PlatformMemory {

    /**
     * 平台内存访问实现
     * Platform memory access implementation
     */
    public static final MemoryAccess ACCESS = load();

    private static final String IMPL_CLASS = "cn.itcraft.frogspawn.platform.MemoryAccessImpl";

    private PlatformMemory() {
    }

    private static MemoryAccess load() {
        try {
            Class<?> clazz = Class.forName(IMPL_CLASS);
            return (MemoryAccess) clazz.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to load " + IMPL_CLASS, e);
        }
    }
}

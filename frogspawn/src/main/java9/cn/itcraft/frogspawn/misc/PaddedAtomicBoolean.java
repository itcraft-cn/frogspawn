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
package cn.itcraft.frogspawn.misc;

import jdk.internal.vm.annotation.Contended;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 填充的原子布尔类，用于消除多线程环境下的伪共享问题。
 * 继承自AtomicBoolean，并通过@Contended注解进行内存填充，确保实例独占缓存行。
 * <p>
 * JDK9+ 版本：使用 {@code jdk.internal.vm.annotation.Contended} 替代 JDK8 的
 * {@code sun.misc.Contended}。
 * <p>
 * Padded atomic boolean class to eliminate false sharing in multi-threading environments.
 * Extends AtomicBoolean and uses @Contended annotation for memory padding to ensure cache line isolation.
 * <p>
 * JDK9+ version: uses {@code jdk.internal.vm.annotation.Contended} instead of the
 * JDK8 {@code sun.misc.Contended}.
 *
 * @author Helly Guo
 */
@Contended
public class PaddedAtomicBoolean extends AtomicBoolean {

    private static final long serialVersionUID = 1L;

    /**
     * 构造带有初始值的PaddedAtomicBoolean实例
     * Constructs a PaddedAtomicBoolean with the given initial value
     *
     * @param initialValue 初始布尔值/initial boolean value
     */
    public PaddedAtomicBoolean(boolean initialValue) {
        super(initialValue);
    }

}

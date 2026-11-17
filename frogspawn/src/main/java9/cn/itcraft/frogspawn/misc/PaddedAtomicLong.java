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

import java.util.concurrent.atomic.AtomicLong;

/**
 * 线程安全且消除伪共享的原子长整型类。
 * 通过继承{@link AtomicLong}并使用{@code jdk.internal.vm.annotation.Contended}注解，
 * 确保每个实例独占缓存行，避免多线程环境下的伪共享问题。
 * <p>
 * JDK9+ 版本：使用 {@code jdk.internal.vm.annotation.Contended} 替代 JDK8 的
 * {@code sun.misc.Contended}。
 * <p>
 * A thread-safe atomic long class that eliminates false sharing.
 * By extending {@link AtomicLong} and annotated with
 * {@code jdk.internal.vm.annotation.Contended}, each instance occupies its own cache line
 * to prevent false sharing in multi-threaded environments.
 * <p>
 * JDK9+ version: uses {@code jdk.internal.vm.annotation.Contended} instead of the
 * JDK8 {@code sun.misc.Contended}.
 *
 * @author Helly Guo
 */
@Contended
public class PaddedAtomicLong extends AtomicLong {

    private static final long serialVersionUID = 1L;

    /**
     * 使用指定的初始值创建PaddedAtomicLong实例。
     * <p>
     * Creates a PaddedAtomicLong instance with the specified initial value.
     *
     * @param initialValue 初始值，允许负数 | initialValue the initial value, which may be negative
     */
    public PaddedAtomicLong(long initialValue) {
        super(initialValue);
    }

}

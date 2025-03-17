/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the License); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an AS IS BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.itcraft.frogspawn.misc;

import sun.misc.Contended;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 填充的原子布尔类，用于消除多线程环境下的伪共享问题。
 * 继承自AtomicBoolean，并通过@Contended注解进行内存填充，确保实例独占缓存行。
 * 
 * Padded atomic boolean class to eliminate false sharing in multi-threading environments.
 * Extends AtomicBoolean and uses @Contended annotation for memory padding to ensure cache line isolation.
 * 
 * 伪共享(false sharing)：当多个线程同时修改同一缓存行中的不同变量时，
 * 会导致不必要的缓存一致性流量，从而显著降低性能。
 * 
 * False sharing: When multiple threads modify different variables in the same cache line,
 * it causes unnecessary cache coherence traffic, significantly degrading performance.
 * 
 * @Contended 注解会触发JVM自动进行内存填充（默认128字节），
 * 需要配合JVM参数-XX:-RestrictContended使用
 * 
 * @Contended annotation triggers JVM automatic memory padding (default 128 bytes),
 * requires JVM parameter -XX:-RestrictContended
 */
@Contended
public class PaddedAtomicBoolean extends AtomicBoolean {
    
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

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

import java.util.concurrent.atomic.AtomicLong;

/**
 * 线程安全且消除伪共享的原子长整型类。
 * 通过继承{@link AtomicLong}并使用{@link sun.misc.Contended}注解，
 * 确保每个实例独占缓存行，避免多线程环境下的伪共享问题。
 * <p>
 * 伪共享（False Sharing）是当多个线程同时修改同一个缓存行中的不同变量时，
 * 导致不必要的缓存一致性流量，进而显著降低并发性能的现象。
 * <p>
 * 适用场景：
 * - 高并发场景中需要频繁修改的计数器（如实时请求计数器）
 * - 高性能并发数据结构（如无锁队列的指针状态）
 * - 需要精确统计的性能指标（如延迟统计、吞吐量统计）
 * <p>
 * A thread-safe atomic long class that eliminates false sharing.
 * By extending {@link AtomicLong} and annotated with {@link sun.misc.Contended},
 * each instance occupies its own cache line to prevent false sharing in multi-threaded environments.
 * <p>
 * False Sharing occurs when multiple threads frequently modify different variables
 * that happen to reside in the same cache line, causing unnecessary cache coherence
 * traffic and significantly degrading concurrency performance.
 * <p>
 * Typical use cases:
 * - High-concurrency counters (e.g., real-time request counters)
 * - High-performance concurrent data structures (e.g., lock-free queue pointers)
 * - Performance metrics requiring precise measurement (e.g., latency stats, throughput stats)
 */
@Contended
public class PaddedAtomicLong extends AtomicLong {

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

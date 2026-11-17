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
package cn.itcraft.frogspawn.offheap;

import cn.itcraft.frogspawn.ObjectsMemoryPool;
import cn.itcraft.frogspawn.ObjectsMemoryPoolFactory;
import cn.itcraft.frogspawn.PoolConfig;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * 超大对象（1MB）四场景 JMH 基准
 * 1MB large-object JMH benchmark with four scenarios
 *
 * 对比 Heap / OffHeap / FixedOffHeap / new。
 * Compares Heap / OffHeap / FixedOffHeap / new.
 *
 * @author Helly Guo
 */
@BenchmarkMode({Mode.Throughput})
@Fork(value = 3, jvmArgs = {"-Xmx4G", "-Xms4G", "-Xmn2G", "-Dfrogspawn.cache.capacity=64",
        "-XX:-RestrictContended"})
@Threads(value = 1)
@Warmup(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 10, timeUnit = TimeUnit.MILLISECONDS)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
public class LargeObjectBenchmark {

    private static final int CAPACITY = 256;
    private static final long TOUCH_VALUE = 1L;

    private static final ObjectsMemoryPool<LargeHeapObject> HEAP_POOL =
            ObjectsMemoryPoolFactory.newPool(LargeHeapObject::new, PoolConfig.heap(CAPACITY));

    private static final ObjectsMemoryPool<LargeOffHeapObject> OFF_HEAP_POOL =
            ObjectsMemoryPoolFactory.newPool(LargeOffHeapObject::new, PoolConfig.offHeap(CAPACITY));

    private static final ObjectsMemoryPool<LargeFixedOffHeapObject> FIXED_OFF_HEAP_POOL =
            ObjectsMemoryPoolFactory.newPool(LargeFixedOffHeapObject::new,
                    PoolConfig.fixedOffHeap(CAPACITY, LargeFixedOffHeapObject.OBJECT_SIZE));

    @Benchmark
    public void test0HeapFetchAndRelease(Blackhole blackhole) {
        LargeHeapObject obj = HEAP_POOL.fetch();
        obj.touch(TOUCH_VALUE);
        blackhole.consume(obj.read());
        HEAP_POOL.release(obj);
    }

    @Benchmark
    public void test1OffHeapFetchAndRelease(Blackhole blackhole) {
        LargeOffHeapObject obj = OFF_HEAP_POOL.fetch();
        obj.touch(TOUCH_VALUE);
        blackhole.consume(obj.read());
        OFF_HEAP_POOL.release(obj);
    }

    @Benchmark
    public void test2FixedOffHeapFetchAndRelease(Blackhole blackhole) {
        LargeFixedOffHeapObject obj = FIXED_OFF_HEAP_POOL.fetch();
        obj.touch(TOUCH_VALUE);
        blackhole.consume(obj.read());
        FIXED_OFF_HEAP_POOL.release(obj);
    }

    @Benchmark
    public void test3New(Blackhole blackhole) {
        LargeHeapObject obj = new LargeHeapObject();
        obj.touch(TOUCH_VALUE);
        blackhole.consume(obj.read());
    }
}

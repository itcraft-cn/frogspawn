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
 * 堆池 / 非堆池 / 固定非堆池 / new 四场景统一基准
 * Unified benchmark for heap / offheap / fixed-offheap / new
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
public class AllPoolsBenchmark {

    private static final int HEAP_SIZE = 3000;
    private static final int FIXED_CAPACITY = 10000;
    private static final int FIXED_OBJECT_SIZE = 64;

    private static final ObjectsMemoryPool<SimplePrimitiveStructure> HEAP_POOL =
            ObjectsMemoryPoolFactory.newPool(SimplePrimitiveStructure::new, HEAP_SIZE);

    private static final ObjectsMemoryPool<SimplePrimitiveStructure> OFF_HEAP_POOL =
            ObjectsMemoryPoolFactory.newOffHeapPool(SimplePrimitiveStructure::new);

    private static final ObjectsMemoryPool<SimpleFixedObject> FIXED_OFF_HEAP_POOL =
            ObjectsMemoryPoolFactory.newFixedOffHeapPool(
                    SimpleFixedObject::new, FIXED_CAPACITY, FIXED_OBJECT_SIZE);

    @Benchmark
    public void test0HeapFetchAndRelease(Blackhole blackhole) {
        SimplePrimitiveStructure obj = HEAP_POOL.fetch();
        blackhole.consume(obj);
        HEAP_POOL.release(obj);
    }

    @Benchmark
    public void test1OffHeapFetchAndRelease(Blackhole blackhole) {
        SimplePrimitiveStructure obj = OFF_HEAP_POOL.fetch();
        blackhole.consume(obj);
        OFF_HEAP_POOL.release(obj);
    }

    @Benchmark
    public void test2FixedOffHeapFetchAndRelease(Blackhole blackhole) {
        SimpleFixedObject obj = FIXED_OFF_HEAP_POOL.fetch();
        blackhole.consume(obj);
        FIXED_OFF_HEAP_POOL.release(obj);
    }

    @Benchmark
    public void test3New(Blackhole blackhole) {
        blackhole.consume(new SimplePrimitiveStructure());
    }
}

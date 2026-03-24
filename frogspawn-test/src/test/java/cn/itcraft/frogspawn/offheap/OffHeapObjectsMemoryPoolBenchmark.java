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

@BenchmarkMode({Mode.Throughput})
@Fork(value = 3, jvmArgs = {"-Xmx4G", "-Xms4G", "-Xmn2G", "-Dfrogspawn.cache.capacity=64", "-XX:-RestrictContended"})
@Threads(value = 1)
@Warmup(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 10, timeUnit = TimeUnit.MILLISECONDS)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
public class OffHeapObjectsMemoryPoolBenchmark {

    private static final ObjectsMemoryPool<SimplePrimitiveStructure> O_H_POOL
            = ObjectsMemoryPoolFactory.newOffHeapPool(SimplePrimitiveStructure::new);
    
    private static final ObjectsMemoryPool<SimplePrimitiveStructure> HEAP_POOL
            = ObjectsMemoryPoolFactory.newPool(SimplePrimitiveStructure::new, 3000);

    @Benchmark
    public void test0OffHeapFetchAndRelease(Blackhole blackhole) {
        SimplePrimitiveStructure pojo = O_H_POOL.fetch();
        blackhole.consume(pojo);
        O_H_POOL.release(pojo);
    }

    @Benchmark
    public void test1HeapFetchAndRelease(Blackhole blackhole) {
        SimplePrimitiveStructure pojo = HEAP_POOL.fetch();
        blackhole.consume(pojo);
        HEAP_POOL.release(pojo);
    }

    @Benchmark
    public void test2New(Blackhole blackhole) {
        blackhole.consume(new SimplePrimitiveStructure());
    }
}

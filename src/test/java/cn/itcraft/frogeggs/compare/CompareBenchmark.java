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
package cn.itcraft.frogeggs.compare;

import cn.itcraft.frogeggs.DemoPojo;
import cn.itcraft.frogeggs.DemoPojoCreator;
import cn.itcraft.frogeggs.impl.CachedLoopObjectsMemoryPoolImpl;
import cn.itcraft.frogeggs.ObjectsMemoryPool;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@Fork(value = 3, jvmArgsPrepend = {
        "-Xmx400m",
        "-Xms400m",
        "-XX:-RestrictContended"
})
@Threads(value = 8)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class CompareBenchmark {
    private static final int ARRAY_SIZE = 32;

    private static final ObjectsMemoryPool<DemoPojo> TE_POOL
            = new CachedLoopObjectsMemoryPoolImpl<>(new DemoPojoCreator(), 3000);

    private static final ThreadLocal<DemoPojo[]> DEMO_POJO_ARRAY_LOCAL =
            ThreadLocal.withInitial(() -> new DemoPojo[ARRAY_SIZE]);


    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(CompareBenchmark.class.getSimpleName())
                .addProfiler(GCProfiler.class)
                .build();

        new Runner(opt).run();
    }

    @Benchmark
    public void testTurtleEggsPool(Blackhole blackhole) {
        DemoPojo pojo = null;
        try {
            pojo = TE_POOL.fetch();
            blackhole.consume(pojo);
        } catch (Exception e) {
            //
        } finally {
            TE_POOL.release(pojo);
        }
    }

    @Benchmark
    public void testNew(Blackhole blackhole) {
        blackhole.consume(new DemoPojo());
    }

    @Benchmark
    @OperationsPerInvocation(ARRAY_SIZE)
    public void testTurtleEggsPoolOneRequestMultiTimes(Blackhole blackhole) {
        DemoPojo[] pojoArray = DEMO_POJO_ARRAY_LOCAL.get();
        try {
            for (int i = 0; i < ARRAY_SIZE; i++) {
                pojoArray[i] = TE_POOL.fetch();
            }
            blackhole.consume(pojoArray);
        } catch (Exception e) {
            //
        } finally {
            for (DemoPojo demoPojo : pojoArray) {
                TE_POOL.release(demoPojo);
            }
        }
    }

    @Benchmark
    @OperationsPerInvocation(ARRAY_SIZE)
    public void testNewOneRequestMultiTimes(Blackhole blackhole) {
        DemoPojo[] pojoArray = DEMO_POJO_ARRAY_LOCAL.get();
        for (int i = 0; i < ARRAY_SIZE; i++) {
            pojoArray[i] = new DemoPojo();
        }
        blackhole.consume(pojoArray);
    }

    @TearDown
    public void tearDown() throws Exception {
    }

}

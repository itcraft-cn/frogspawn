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
package cn.itcraft.frogspawn;

import cn.itcraft.frogspawn.strategy.FetchFailStrategy;
import cn.itcraft.frogspawn.strategy.FetchStrategy;
import cn.itcraft.frogspawn.strategy.PoolStrategy;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
public class ObjectsMemoryPoolBenchmark {

    private static final ObjectsMemoryPool<DemoPojo> POOL
            = ObjectsMemoryPoolFactory.newPool(new DemoPojoCreator(), 3000,
                                               new PoolStrategy(FetchStrategy.FETCH_FAIL_AS_NEW,
                                                                FetchFailStrategy.CALL_CREATOR, true));

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(ObjectsMemoryPoolBenchmark.class.getSimpleName())
                .mode(Mode.Throughput)
                .forks(3)
                .threads(1)
                .warmupIterations(10)
                .warmupTime(TimeValue.seconds(1))
                .measurementIterations(10)
                .measurementTime(TimeValue.seconds(1))
                .timeUnit(TimeUnit.MICROSECONDS)
                .jvmArgs("-Xmx4G", "-Xms4G", "-Xmn2G", "-Dfrogspawn.cache.capacity=64", "-XX:-RestrictContended")
                .addProfiler(GCProfiler.class)
                //.addProfiler(StackProfiler.class)
                //.addProfiler(CompilerProfiler.class)
                //.addProfiler(LinuxPerfNormProfiler.class)
                //.addProfiler(LinuxPerfNormProfiler.class).verbosity(VerboseMode.EXTRA)
                .build();

        new Runner(opt).run();
    }

    @Benchmark
    public void testFetchAndRelease(Blackhole blackhole) {
        DemoPojo pojo = POOL.fetch();
        blackhole.consume(pojo);
        POOL.release(pojo);
    }

    @Benchmark
    public void testNew(Blackhole blackhole) {
        blackhole.consume(new DemoPojo());
    }
}

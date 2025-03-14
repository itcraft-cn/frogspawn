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
package cn.itcraft.frogeggs;

import cn.itcraft.frogeggs.strategy.PoolStrategy;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.concurrent.TimeUnit;

/**
 * @author Helly Guo
 * <p>
 * Created on 1/26/22 2:58 PM
 */
@State(Scope.Benchmark)
public class NormalVsAutofillPoolBenchmark {

    private static final int TEST_TIMES = 10000;
    private static final int SIZE = 3000000;

    private static final ObjectsMemoryPool<DemoPojo> SINGLE_POOL =
            ObjectsMemoryPoolFactory.newPool(new DemoPojoCreator(), SIZE, PoolStrategy.FETCH_FAIL_AS_NEW, false);
    private static final ObjectsMemoryPool<DemoPojo> DRAIN_POOL =
            ObjectsMemoryPoolFactory.newPool(new DemoPojoCreator(), SIZE, PoolStrategy.FETCH_FAIL_AS_NEW, true);

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(NormalVsAutofillPoolBenchmark.class.getSimpleName())
                .mode(Mode.Throughput)
                .forks(3)
                .threads(1)
                .warmupIterations(10)
                .warmupTime(TimeValue.seconds(1))
                .measurementIterations(10)
                .measurementTime(TimeValue.seconds(1))
                .timeUnit(TimeUnit.MICROSECONDS)
                .jvmArgs("-Xmx4G", "-Xms4G", "-Xmn2G", "-Dturtleeggs.cache.capacity=64")
                .build();
        new Runner(options).run();
    }

    static void execute(Blackhole hole, ObjectsMemoryPool<DemoPojo> pool) {
        hole.consume(NormalVsAutofillPoolExecutor.execute(pool, TEST_TIMES).intValue());
    }

    @Benchmark
    @OperationsPerInvocation(TEST_TIMES)
    public void testSingle(Blackhole hole) {
        execute(hole, SINGLE_POOL);
    }

    @Benchmark
    @OperationsPerInvocation(TEST_TIMES)
    public void testDrain(Blackhole hole) {
        execute(hole, DRAIN_POOL);
    }
}

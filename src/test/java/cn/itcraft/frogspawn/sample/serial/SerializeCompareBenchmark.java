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
package cn.itcraft.frogspawn.sample.serial;

import cn.itcraft.frogspawn.DemoPojo;
import cn.itcraft.frogspawn.DemoPojoCreator;
import cn.itcraft.frogspawn.Resettable;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
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
@Fork(value = 3, jvmArgsPrepend = {"-Xmx4096m", "-Xms4096m", "-XX:-RestrictContended"})
@Threads(value = 8)
@Warmup(iterations = 5, time = 1)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Measurement(iterations = 10, time = 1)
public class SerializeCompareBenchmark {
    private static final int HASH_CODE = DemoPojo.class.getName().hashCode();

    private static final byte[] BYTES1;
    private static final byte[] BYTES2;

    static {
        Serializer.registerClass(DemoPojo.class);
        SerializerWithObjectPool.registerClass(DemoPojo.class, new DemoPojoCreator());
        DemoPojo demo = new DemoPojo();
        demo.setVal1(1);
        demo.setVal2(2L);
        demo.setVal3(3D);
        demo.setVal4("hello");
        BYTES1 = Serializer.serialize(demo, HASH_CODE);
        BYTES2 = SerializerWithObjectPool.serialize(demo, HASH_CODE);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(SerializeCompareBenchmark.class.getSimpleName())
                .addProfiler(GCProfiler.class)
                //.addProfiler(StackProfiler.class)
                //.addProfiler(CompilerProfiler.class)
                //.addProfiler(LinuxPerfNormProfiler.class)
                //.addProfiler(LinuxPerfNormProfiler.class).verbosity(VerboseMode.EXTRA)
                .build();

        new Runner(opt).run();
    }

    @Benchmark
    public void testDeserialization1(Blackhole blackhole) {
        blackhole.consume(Serializer.deserialize(BYTES1, HASH_CODE));
    }

    @Benchmark
    public void testDeserialization2(Blackhole blackhole) {
        Resettable obj = null;
        try {
            obj = SerializerWithObjectPool.deserialize(BYTES2, HASH_CODE);
            blackhole.consume(obj);
        } finally {
            SerializerWithObjectPool.release(obj, HASH_CODE);
        }
    }
}

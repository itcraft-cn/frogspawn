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

import cn.itcraft.frogspawn.misc.MainHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObjectsMemoryPoolSample {

    private static final Logger LOGGER = LoggerFactory.getLogger(ObjectsMemoryPoolSample.class);

    private static final ObjectsMemoryPool<DemoPojo> POOL
            = ObjectsMemoryPoolFactory.newPool(new DemoPojoCreator(), 3000);
    private static final int SIZE = 256;
    private static final int MASK = SIZE - 1;

    public static void main(String[] args) {
        ObjectsMemoryPoolSample sample = new ObjectsMemoryPoolSample();
        MainHelper.waitProfiler();
        sample.loop(100_000);
        sample.loop(1_000_000);
        sample.loop2(100_000);
        sample.loop2(1_000_000);
        MainHelper.holdJvm();
    }

    public void loop(int n) {
        long start = System.nanoTime();
        for (int i = 0; i < n; i++) {
            POOL.release(POOL.fetch());
        }
        long end = System.nanoTime();
        LOGGER.info("cost: {}ns", (end - start));
    }

    public void loop2(int n) {
        long start = System.nanoTime();
        DemoPojo[] array = new DemoPojo[SIZE];
        for (int i = 0; i < SIZE; i++) {
            array[i] = POOL.fetch();
        }
        long walker = SIZE;
        int idx;
        for (int i = SIZE; i < n; i++) {
            idx = (int) ((walker++) % MASK);
            POOL.release(array[idx]);
            array[idx] = POOL.fetch();
        }

        for (int i = 0; i < SIZE; i++) {
            POOL.release(array[i]);
        }
        long end = System.nanoTime();
        LOGGER.info("cost: {}ns", (end - start));
    }
}

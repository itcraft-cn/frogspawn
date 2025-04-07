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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Helly Guo
 * <p>
 * Created on 8/25/21 12:59 AM
 */
public class HeapObjectsMemoryPoolTest {

    public static final int SINGLE_TEST_TIMES = 300000;
    public static final int MULTI_TEST_TIMES = 300;
    public static final int SINGLE_CAPACITY = 3000;
    public static final int MULTI_CAPACITY = 50;
    private static final Logger LOGGER = LoggerFactory.getLogger(HeapObjectsMemoryPoolTest.class);

    @Test
    public void test() {
        ObjectsMemoryPool<DemoPojo> pojoPool
                = ObjectsMemoryPoolFactory.newPool(new DemoPojoCreator(), SINGLE_CAPACITY);
        Map<Integer, AtomicLong> countMap = new HashMap<>();
        for (int i = 0; i < SINGLE_TEST_TIMES; i++) {
            usingDemo(pojoPool, countMap, false);
        }
        Assertions.assertTrue(countMap.values().stream().anyMatch(v -> v.get() > 1));
        countMap.entrySet().forEach(v -> LOGGER.info("count: {}", v));
    }

    @Test
    public void testHold() {
        ThreadPoolExecutor pool = new ThreadPoolExecutor(8, 8,
                                                         1000L, TimeUnit.MILLISECONDS,
                                                         new ArrayBlockingQueue<>(MULTI_TEST_TIMES),
                                                         new DemoThreadFactory("demo"),
                                                         new ThreadPoolExecutor.DiscardOldestPolicy());
        pool.prestartAllCoreThreads();
        ObjectsMemoryPool<DemoPojo> pojoPool
                = ObjectsMemoryPoolFactory.newPool(new DemoPojoCreator(), MULTI_CAPACITY);
        Map<Integer, AtomicLong> countMap = new HashMap<>();
        List<Callable<Integer>> tasks = new ArrayList<>(MULTI_TEST_TIMES);
        for (int i = 0; i < MULTI_TEST_TIMES; i++) {
            tasks.add(() -> usingDemo(pojoPool, countMap, true));
        }
        try {
            List<Future<Integer>> futures = pool.invokeAll(tasks);
            while (!futures.stream().allMatch(Future::isDone)) {
                trySleep(10L);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Assertions.assertTrue(countMap.values().stream().anyMatch(v -> v.get() > 1));
        countMap.entrySet().forEach(v -> LOGGER.info("count: {}", v));
    }

    private int usingDemo(ObjectsMemoryPool<DemoPojo> pojoPool, Map<Integer, AtomicLong> countMap, boolean sleep) {
        DemoPojo pojo = pojoPool.fetch();
        countMap.computeIfAbsent(pojo.hashCode(), k -> new AtomicLong(0)).incrementAndGet();
        if (sleep) {
            trySleep(ThreadLocalRandom.current().nextInt(25) * 10);
        }
        pojoPool.release(pojo);
        return 0;
    }

    private void trySleep(long interval) {
        try {
            Thread.sleep(interval);
        } catch (InterruptedException e) {
            //
        }
    }

}

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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 * @author Helly Guo
 * <p>
 * Created on 1/26/22 2:58 PM
 */
public class NormalVsAutofillPoolExecutor {

    private static final int TEST_TIMES = 10000;
    private static final int SIZE = 3000000;

    private static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(2, 2,
                                                                              1000, TimeUnit.MILLISECONDS,
                                                                              new ArrayBlockingQueue<>(4),
                                                                              new DemoThreadFactory("demo"),
                                                                              new ThreadPoolExecutor.AbortPolicy());
    private static final BlockingQueue<DemoPojo> QUEUE = new ArrayBlockingQueue<>(SIZE);

    static LongAdder execute(ObjectsMemoryPool<DemoPojo> pool, int times) {
        LongAdder sum = new LongAdder();
        CountDownLatch latch = new CountDownLatch(times);
        EXECUTOR.submit(() -> send(pool, QUEUE));
        EXECUTOR.submit(() -> receive(pool, sum, latch, QUEUE));
        waitDone(latch);
        return sum;
    }

    private static void send(ObjectsMemoryPool<DemoPojo> pool, BlockingQueue<DemoPojo> queue) {
        DemoPojo pojo;
        for (int i = 0; i < TEST_TIMES; i++) {
            pojo = pool.fetch();
            pojo.setVal1(1);
            queue.offer(pojo);
        }
    }

    private static void receive(ObjectsMemoryPool<DemoPojo> pool, LongAdder sum, CountDownLatch latch,
                                BlockingQueue<DemoPojo> queue) {
        DemoPojo pojo;
        for (int i = 0; i < TEST_TIMES; i++) {
            try {
                pojo = queue.take();
            } catch (InterruptedException e) {
                continue;
            }
            sum.add(pojo.getVal1());
            pool.release(pojo);
            latch.countDown();
        }
    }

    static void waitDone(CountDownLatch latch) {
        try {
            while (true) {
                if (latch.await(1000L, TimeUnit.MILLISECONDS)) {
                    break;
                }
            }
        } catch (InterruptedException e) {
            //
        }
    }
}

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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Helly Guo
 * <p>
 * Created on 8/25/21 12:59 AM
 */
public class HeapObjectsMemoryPool2Test {

    public static final int SINGLE_TEST_TIMES = 300000;
    public static final int SINGLE_CAPACITY = 3000;
    private static final Logger LOGGER = LoggerFactory.getLogger(HeapObjectsMemoryPoolTest.class);

    @Test
    public void testDefault() {
        ObjectsMemoryPool<DemoPojo> pojoPool
                = ObjectsMemoryPoolFactory.newPool(new DemoPojoCreator(), SINGLE_CAPACITY);
        test(pojoPool);
    }

    private void test(ObjectsMemoryPool<DemoPojo> pojoPool) {
        Map<Integer, AtomicLong> countMap = new HashMap<>();
        for (int i = 0; i < SINGLE_TEST_TIMES; i++) {
            usingDemo(pojoPool, countMap);
        }
        Assertions.assertTrue(countMap.values().stream().anyMatch(v -> v.get() > 1));
        countMap.entrySet().forEach(v -> LOGGER.info("count: {}", v));
    }

    private void usingDemo(ObjectsMemoryPool<DemoPojo> pojoPool, Map<Integer, AtomicLong> countMap) {
        DemoPojo pojo = pojoPool.fetch();
        countMap.computeIfAbsent(pojo.hashCode(), k -> new AtomicLong(0)).incrementAndGet();
        pojoPool.release(pojo);
    }
}

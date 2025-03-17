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
import cn.itcraft.frogspawn.strategy.FetchFailStrategy;
import cn.itcraft.frogspawn.strategy.PoolStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * @author Helly Guo
 * <p>
 * Created on 1/26/22 2:58 PM
 */
public class CachedVsPrefetchPoolSample {

    private static final Logger LOGGER = LoggerFactory.getLogger(CachedVsPrefetchPoolSample.class);

    private static final int PREHOT_TIMES = 10000;
    private static final int TEST_TIMES = 10000;
    private static final int SIZE = 30000;

    public static void main(String[] args) {
        PoolStrategy strategy1 = new PoolStrategy(FetchFailStrategy.CALL_CREATOR, false);
        PoolStrategy strategy2 = new PoolStrategy(FetchFailStrategy.CALL_CREATOR, true);
        ObjectsMemoryPool<DemoPojo> singlePool =
                ObjectsMemoryPoolFactory.newPool(new DemoPojoCreator(), SIZE, strategy1);
        ObjectsMemoryPool<DemoPojo> drainPool =
                ObjectsMemoryPoolFactory.newPool(new DemoPojoCreator(), SIZE, strategy2);
        MainHelper.waitProfiler();
        prehot(singlePool);
        prehot(drainPool);
        Object[] logInfoArray1 = test(singlePool);
        Object[] logInfoArray2 = test(drainPool);
        LOGGER.info("{}", Arrays.toString(logInfoArray1));
        LOGGER.info("{}", Arrays.toString(logInfoArray2));
        MainHelper.holdJvm();
    }

    private static void prehot(ObjectsMemoryPool<DemoPojo> pool) {
        LOGGER.info("{} prehot started", pool.getClass().getName());
        CachedVsPrefetchPoolExecutor.execute(pool, PREHOT_TIMES);
    }

    private static Object[] test(ObjectsMemoryPool<DemoPojo> pool) {
        LOGGER.info("{} test started", pool.getClass().getName());
        long start = System.currentTimeMillis();
        CachedVsPrefetchPoolExecutor.execute(pool, TEST_TIMES);
        long end = System.currentTimeMillis();
        return new Object[]{pool.getClass().getName(), end - start, 1D * TEST_TIMES / ((end - start) / 1000D)};
    }
}

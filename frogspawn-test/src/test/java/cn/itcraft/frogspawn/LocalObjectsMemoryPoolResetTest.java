/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.itcraft.frogspawn;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * 堆内存池释放重置语义测试
 * Heap pool release/reset semantics test
 *
 * 校验对象进入线程本地缓存前也会被重置，符合 ObjectsMemoryPool.release 的约定。
 * Verifies objects are reset before entering the thread-local cache, as promised
 * by ObjectsMemoryPool.release.
 *
 * @author Helly Guo
 */
class LocalObjectsMemoryPoolResetTest {

    private static final int POOL_SIZE = 2;
    private static final int SAMPLE_INT = 123;
    private static final long SAMPLE_LONG = 456L;

    @Test
    void releaseShouldResetObjectInThreadLocalCache() {
        ObjectsMemoryPool<DemoPojo> pool =
                ObjectsMemoryPoolFactory.newPool(new DemoPojoCreator(), POOL_SIZE);

        DemoPojo first = pool.fetch();
        first.setVal1(SAMPLE_INT);
        first.setVal2(SAMPLE_LONG);
        pool.release(first);

        DemoPojo second = pool.fetch();
        assertSame(first, second);
        assertEquals(0, second.getVal1());
        assertEquals(0L, second.getVal2());
    }
}

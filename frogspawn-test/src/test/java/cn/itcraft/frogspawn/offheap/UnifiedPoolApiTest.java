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
package cn.itcraft.frogspawn.offheap;

import cn.itcraft.frogspawn.DemoPojo;
import cn.itcraft.frogspawn.DemoPojoCreator;
import cn.itcraft.frogspawn.ObjectsMemoryPool;
import cn.itcraft.frogspawn.ObjectsMemoryPoolFactory;
import cn.itcraft.frogspawn.PoolConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 统一池创建 API 测试
 * Unified pool creation API test
 *
 * @author Helly Guo
 */
class UnifiedPoolApiTest {

    private static final int CAPACITY = 16;
    private static final int FIXED_CAPACITY = 4;
    private static final int OBJECT_SIZE = 64;

    @Test
    void shouldCreateHeapPoolByConfig() {
        ObjectsMemoryPool<DemoPojo> pool = ObjectsMemoryPoolFactory.newPool(
                new DemoPojoCreator(), PoolConfig.heap(CAPACITY));

        DemoPojo obj = pool.fetch();
        assertNotNull(obj);
        pool.release(obj);
    }

    @Test
    void shouldCreateOffHeapPoolByConfig() {
        ObjectsMemoryPool<SimplePrimitiveStructure> pool = ObjectsMemoryPoolFactory.newPool(
                SimplePrimitiveStructure::new, PoolConfig.offHeap(CAPACITY));

        SimplePrimitiveStructure obj = pool.fetch();
        assertNotNull(obj);
        pool.release(obj);
    }

    @Test
    void shouldCreateFixedOffHeapPoolByConfig() throws Exception {
        ObjectsMemoryPool<SimpleFixedObject> pool = ObjectsMemoryPoolFactory.newPool(
                SimpleFixedObject::new, PoolConfig.fixedOffHeap(FIXED_CAPACITY, OBJECT_SIZE));
        try {
            SimpleFixedObject obj = pool.fetch();
            assertNotNull(obj);
            pool.release(obj);
        } finally {
            ((AutoCloseable) pool).close();
        }
    }

    @Test
    void shouldRejectWrongTypeForOffHeapPool() {
        assertThrows(IllegalArgumentException.class,
                     () -> ObjectsMemoryPoolFactory.newPool(
                             new DemoPojoCreator(), PoolConfig.offHeap(CAPACITY)));
    }

    @Test
    void shouldRejectWrongTypeForFixedOffHeapPool() {
        assertThrows(IllegalArgumentException.class,
                     () -> ObjectsMemoryPoolFactory.newPool(
                             SimplePrimitiveStructure::new,
                             PoolConfig.fixedOffHeap(FIXED_CAPACITY, OBJECT_SIZE)));
    }
}

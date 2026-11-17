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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 固定非堆对象池边界与归属校验测试
 * Fixed off-heap pool boundary and ownership validation test
 *
 * @author Helly Guo
 */
class FixedOffHeapPoolTest {

    private static final int CAPACITY = 2;
    private static final int OBJECT_SIZE = 64;

    private FixedOffHeapPool<SimpleFixedObject> newPool() {
        return new FixedOffHeapPool<>(SimpleFixedObject::new, CAPACITY, OBJECT_SIZE);
    }

    @Test
    void shouldRejectFetchWhenExhausted() {
        try (FixedOffHeapPool<SimpleFixedObject> pool = newPool()) {
            pool.fetch();
            pool.fetch();

            assertThrows(IllegalStateException.class, pool::fetch);
        }
    }

    @Test
    void shouldAllowFetchAfterRelease() {
        try (FixedOffHeapPool<SimpleFixedObject> pool = newPool()) {
            SimpleFixedObject first = pool.fetch();
            SimpleFixedObject second = pool.fetch();

            pool.release(first);

            assertSame(first, pool.fetch());
            pool.release(second);
        }
    }

    @Test
    void shouldRejectDoubleRelease() {
        try (FixedOffHeapPool<SimpleFixedObject> pool = newPool()) {
            SimpleFixedObject obj = pool.fetch();
            pool.release(obj);

            assertThrows(IllegalArgumentException.class, () -> pool.release(obj));
        }
    }

    @Test
    void shouldRejectForeignObject() {
        try (FixedOffHeapPool<SimpleFixedObject> pool = newPool()) {
            SimpleFixedObject foreign = new SimpleFixedObject();
            foreign.index(0);

            assertThrows(IllegalArgumentException.class, () -> pool.release(foreign));
        }
    }
}

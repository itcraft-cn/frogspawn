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

import cn.itcraft.frogspawn.platform.PlatformMemory;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 固定非堆对象 ByteBuffer 视图测试
 * Fixed off-heap object ByteBuffer view test
 *
 * @author Helly Guo
 */
class FixedOffHeapBufferTest {

    private static final int CAPACITY = 2;
    private static final int OBJECT_SIZE = 64;
    private static final int JDK24_VERSION = 24;

    static final class BufferAwareFixedObject extends SimpleFixedObject {
        @Override
        public ByteBuffer buffer() {
            return PlatformMemory.asByteBuffer(address(), OBJECT_SIZE);
        }
    }

    @Test
    void defaultBufferShouldBeUnsupported() {
        assertThrows(UnsupportedOperationException.class,
                     () -> new SimpleFixedObject().buffer());
    }

    @Test
    void jdk24ShouldProvideZeroCopyBufferView() {
        if (currentJavaVersion() < JDK24_VERSION) {
            assertThrows(UnsupportedOperationException.class,
                         () -> PlatformMemory.asByteBuffer(0L, OBJECT_SIZE));
            return;
        }

        try (FixedOffHeapPool<BufferAwareFixedObject> pool =
                     new FixedOffHeapPool<>(BufferAwareFixedObject::new, CAPACITY, OBJECT_SIZE)) {
            BufferAwareFixedObject obj = pool.fetch();
            ByteBuffer buffer = obj.buffer();

            buffer.putLong(0, 42L);
            assertEquals(42L, buffer.getLong(0));

            pool.release(obj);
        }
    }

    private static int currentJavaVersion() {
        String version = System.getProperty("java.specification.version");
        if (version.startsWith("1.")) {
            return Integer.parseInt(version.substring(2));
        }
        int dotIndex = version.indexOf('.');
        return Integer.parseInt(dotIndex < 0 ? version : version.substring(0, dotIndex));
    }
}

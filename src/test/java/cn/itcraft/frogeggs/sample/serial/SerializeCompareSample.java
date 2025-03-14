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
package cn.itcraft.frogeggs.sample.serial;

import cn.itcraft.frogeggs.DemoPojo;
import cn.itcraft.frogeggs.DemoPojoCreator;
import cn.itcraft.frogeggs.Resettable;
import cn.itcraft.frogeggs.misc.MainHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SerializeCompareSample {

    private static final Logger LOGGER = LoggerFactory.getLogger(SerializeCompareSample.class);

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

    public static void main(String[] args) {
        MainHelper.waitProfiler();
        // pre hot
        execute(100000);
        // test
        execute(20000000);
        MainHelper.holdJvm();
    }

    private static void execute(int n) {
        long start = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            testDeserialization1();
        }
        long end = System.currentTimeMillis();
        LOGGER.info("cost: {}ms", (end - start));
        start = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            testDeserialization2();
        }
        end = System.currentTimeMillis();
        LOGGER.info("cost: {}ms", (end - start));
    }

    private static void testDeserialization1() {
        Serializer.deserialize(BYTES1, HASH_CODE);
    }

    private static void testDeserialization2() {
        Resettable obj = null;
        try {
            obj = SerializerWithObjectPool.deserialize(BYTES2, HASH_CODE);
        } finally {
            SerializerWithObjectPool.release(obj, HASH_CODE);
        }
    }
}

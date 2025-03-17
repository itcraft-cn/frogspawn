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
package cn.itcraft.frogspawn.sample.serial;

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtobufIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 序列化/反序列化工具<br>
 *
 * @author Helly Guo
 * <p>
 * Created on 5/29/20 5:17 PM
 */
public class Serializer {

    private static final Map<Integer, Schema<?>> SCHEMA_MAP = new HashMap<>();
    private static final int BUF_SIZE = 16 * 1024;
    private static final SoftReferenceThreadLocal<LinkedBuffer> BUFFER_LOCAL =
            SoftReferenceThreadLocalCreator.createThreadLocal(() -> LinkedBuffer.allocate(BUF_SIZE));

    private Serializer() {
    }

    /**
     * 注册类
     *
     * @param clazz 类
     * @param <T>   泛型信息
     */
    public static <T> void registerClass(Class<T> clazz) {
        int key = clazz.getName().hashCode();
        if (!SCHEMA_MAP.containsKey(key)) {
            SCHEMA_MAP.put(key, RuntimeSchema.getSchema(clazz));
        }
    }

    /**
     * 序列化
     *
     * @param obj           对象
     * @param classHashCode 该对象对应的类 hashcode
     * @param <T>           泛型信息
     * @return bytearray
     */
    @SuppressWarnings("unchecked")
    public static <T> byte[] serialize(T obj, int classHashCode) {
        Schema<T> schema = (Schema<T>) SCHEMA_MAP.get(classHashCode);
        if (schema == null) {
            throw new RuntimeException("Class with hash code[" + classHashCode + "] is not found, "
                                               + "cannot encode[" + obj + "]");
        } else {
            LinkedBuffer buffer = BUFFER_LOCAL.getCached();
            try {
                return ProtobufIOUtil.toByteArray(obj, schema, buffer);
            } finally {
                buffer.clear();
            }
        }
    }

    /**
     * 反序列化
     *
     * @param bytes         bytearray
     * @param classHashCode 该对象对应的类 hashcode
     * @param <T>           泛型信息
     * @return 转换后的对象
     */
    @SuppressWarnings("unchecked")
    public static <T> T deserialize(byte[] bytes, int classHashCode) {
        Schema<T> schema = (Schema<T>) SCHEMA_MAP.get(classHashCode);
        if (schema == null) {
            throw new RuntimeException("Class with hash code[" + classHashCode + "] is not found, "
                                               + "cannot decode[" + Arrays.toString(bytes) + "]");
        } else {
            T obj = schema.newMessage();
            ProtobufIOUtil.mergeFrom(bytes, obj, schema);
            return obj;
        }
    }
}

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

/**
 * 通过将{@link ThreadLocal}的 Value 也使用{@link java.lang.ref.SoftReference}包装，来保证内存不泄漏
 *
 * @author Helly Guo
 * <p>
 * Created on 12/13/18 3:09 PM
 */
public final class SoftReferenceThreadLocalCreator {

    private SoftReferenceThreadLocalCreator() {
    }

    /**
     * 构建软引用 ThreadLocal {@link SoftReferenceThreadLocal}
     *
     * @param objCreator 对象创建器
     * @param <T>        泛型信息
     * @return {@link SoftReferenceThreadLocal} 对象
     * @see java.lang.ref.SoftReference
     */
    public static <T> SoftReferenceThreadLocal<T> createThreadLocal(SoftRefObjectCreator<T> objCreator) {
        return new SoftReferenceThreadLocal<>(objCreator);
    }

}

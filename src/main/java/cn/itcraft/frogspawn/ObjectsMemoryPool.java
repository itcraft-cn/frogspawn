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

/**
 * 可重置对象的内存池接口，用于管理可重用对象
 * Memory pool interface for resettable objects, manages reusable objects
 *
 * @param <T> 必须实现 Resettable 接口的类型参数，确保对象可被重置
 *            Type parameter that must implement Resettable interface, ensuring objects can be reset
 * @author Helly Guo
 * <p>
 * Created on 8/24/21 11:31 PM
 */
public interface ObjectsMemoryPool<T extends Resettable> {

    /**
     * 从内存池获取可用对象。当池中有可用对象时返回池中对象，否则行为由具体实现决定
     * (可能创建新实例或返回null)
     * <p>
     * Fetches an available object from the memory pool. Returns pooled object when available,
     * otherwise behavior is implementation-dependent (may create new instance or return null)
     *
     * @return 池中的可用对象或新建对象 (具体取决于实现)
     * Available object from pool or new instance (implementation dependent)
     */
    T fetch();

    /**
     * 将使用完毕的对象归还内存池。对象在入池前会自动调用reset()方法进行状态重置
     * (注意：实现类应处理无效对象和对象验证)
     * <p>
     * Returns a used object to the memory pool. The object will be reset by
     * automatically calling reset() method before being pooled.
     * (Note: Implementation should handle invalid objects and object validation)
     *
     * @param used 需要归还的对象 (必须非null且属于本池管理的对象)
     *             The used object to return (must be non-null and managed by this pool)
     * @throws IllegalArgumentException 如果参数不合法（具体异常类型由实现决定）
     *                                  If invalid argument (specific exception type depends on implementation)
     */
    void release(T used);
}

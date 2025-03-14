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

/**
 * @author Helly Guo
 * <p>
 * Created on 8/24/21 11:31 PM
 */
public interface ObjectsMemoryPool<T extends Resettable> {

    /**
     * 获取空闲对象
     *
     * @return 对象
     */
    T fetch();

    /**
     * 返回使用后对象
     *
     * @param used 对象
     */
    void release(T used);

}

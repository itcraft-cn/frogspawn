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
 * @author Helly Guo
 * <p>
 * Created on 8/25/21 12:38 AM
 * 
 * 可重置对象的接口规范
 * Interface specification for resettable objects
 */
public interface Resettable {

    /**
     * 重置对象状态到初始值
     * Reset object state to initial values
     */
    void reset();

    /**
     * 检查对象是否处于无效状态
     * 默认实现始终返回 false（默认有效）
     * 
     * @return 如果对象无效返回 true，否则 false
     *         Returns true if the object is invalid, false otherwise
     * 
     * Check if the object is in invalid state
     * Default implementation always returns false (valid by default)
     */
    default boolean isInvalid() {
        return false;
    }

    /**
     * 获取通过 markId 方法注入的分配标识符
     * 
     * @return 被标记的分配 ID
     *         The marked allocation ID
     * 
     * Get the assigned identifier injected via markId method
     */
    int getMarkedId();

    /**
     * 标记分配的唯一标识符，应由实现类存储该值
     * 
     * @param id 要分配的标识符
     *           The identifier to be assigned
     * 
     * Mark the assigned unique identifier, should be stored by implementing class
     */
    void markId(int id);
}

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
 */
public interface Resettable {

    /**
     * 重置
     */
    void reset();

    /**
     * 是否有效
     */
    default boolean isInvalid() {
        return false;
    }

    /**
     * 返回分配 ID，由 {@link #markId(int)} 注入，返回注入的值即可
     *
     * @return 分配的 ID
     */
    int getMarkedId();

    /**
     * 标记分配到的 ID，请存储在对象内
     *
     * @param id 分配的 ID
     */
    void markId(int id);
}

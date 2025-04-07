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
package cn.itcraft.frogspawn.failrestore;

import cn.itcraft.frogspawn.ObjectCreator;
import cn.itcraft.frogspawn.Resettable;

/**
 * 分布式系统的失败恢复接口
 * Distributed system failure recovery interface
 *
 * @author Helly Guo
 * <p>
 * Created on 8/25/21 3:28 PM
 */
public interface FailRestorer {

    /**
     * 当对象创建失败时执行恢复操作，尝试通过备用策略创建对象
     * Performs failure recovery when object creation fails, attempts to create object through fallback strategy
     *
     * @param creator 用于创建新实例的对象创建器 | Object creator for creating new instances
     * @param <T>     要创建的对象的类型 | Type of the object to be created
     * @return 通过恢复机制创建的对象 | Object created through recovery mechanism
     */
    <T extends Resettable> T failover(ObjectCreator<T> creator);
}

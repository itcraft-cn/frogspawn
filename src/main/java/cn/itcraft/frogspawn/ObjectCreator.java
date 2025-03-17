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
 * 对象创建器接口，定义创建可重置对象的标准方法
 * Object creator interface defining standard methods to create resettable objects
 *
 * @param <T> 可重置对象类型参数，需继承Resettable接口
 *           Type parameter of resettable object, must extend Resettable interface
 * 
 * @author Helly Guo
 * <p>
 * Created on 8/24/21 11:32 PM
 */
public interface ObjectCreator<T extends Resettable> {

    /**
     * 创建并返回一个新的可重置对象实例
     * 实现类需确保返回对象已完成初始化并处于可用状态
     * 
     * Creates and returns a new instance of resettable object
     * Implementation should ensure the returned object is properly initialized and ready to use
     *
     * @return 新创建的可重置对象实例
     *         Newly created resettable object instance
     */
    T create();
}

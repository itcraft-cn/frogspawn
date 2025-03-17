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
package cn.itcraft.frogspawn.impl;

import cn.itcraft.frogspawn.ObjectCreator;
import cn.itcraft.frogspawn.Resettable;

/**
 * 循环预取池实现类，通过循环策略复用对象资源
 * Loop prefetch pool implementation class that reuses object resources through round-robin strategy
 * 
 * @author Helly Guo
 * <p>
 * Created on 8/24/21 11:34 PM
 * 
 * @param <T> 可重置类型的泛型参数，必须继承Resettable接口
 * @param <T> Generic type parameter for resettable objects, must extend Resettable interface
 */
public class PrefetchLoopPoolImpl<T extends Resettable> extends AbstractPrefetchPoolImpl<T> {

    /**
     * 构造函数，初始化对象池
     * Constructor, initializes the object pool
     * 
     * @param creator 对象创建器，用于生成新的池对象
     * @param creator Object creator for generating new pool instances
     * @param size    对象池的固定容量大小
     * @param size     Fixed capacity size of the object pool
     */
    public PrefetchLoopPoolImpl(ObjectCreator<T> creator, int size) {
        super(creator, size);
    }

    /**
     * 获取池中数据的核心方法，使用循环遍历策略
     * Core method for fetching data from pool using round-robin traversal strategy
     * 
     * @return 返回通过循环策略获取的下一个可用对象实例
     * @return Next available object instance obtained through round-robin strategy
     */
    @Override
    protected T fetchData() {
        return FetchHelper.loopFetchData(array, indexMask, walker);
    }
}

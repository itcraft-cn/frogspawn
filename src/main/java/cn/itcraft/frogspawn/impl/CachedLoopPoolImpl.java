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
 * 基于循环策略的缓存对象池实现类，用于高效管理和复用可重置对象
 * Cached object pool implementation with loop strategy, for efficient management and reuse of resettable objects
 *
 * @param <T> 泛型类型，需实现Resettable接口，表示可重置的对象类型 | Generic type that implements Resettable interface, representing resettable object type
 * @author Helly Guo
 * <p>
 * Created on 8/24/21 11:34 PM
 */
public class CachedLoopPoolImpl<T extends Resettable> extends AbstractCachedPool<T> {

    /**
     * 构造方法，初始化缓存对象池
     * Constructor, initializes the cached object pool
     *
     * @param creator 对象创建器，用于生成新的池对象 | Object creator for generating new pool objects
     * @param size    对象池的初始容量大小 | Initial capacity of the object pool
     */
    public CachedLoopPoolImpl(ObjectCreator<T> creator, int size) {
        super(creator, size);
    }

    /**
     * 从对象池中获取可用对象的实现方法，采用循环索引策略
     * Implementation method for fetching available objects from the pool using loop index strategy
     *
     * @return 返回可用的对象实例 | Available object instance
     */
    @Override
    protected T fetchData() {
        return FetchHelper.loopFetchData(array, indexMask, walker);
    }
}

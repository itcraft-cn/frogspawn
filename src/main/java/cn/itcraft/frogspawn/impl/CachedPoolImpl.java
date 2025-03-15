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
import cn.itcraft.frogspawn.strategy.FetchFailStrategy;
import cn.itcraft.frogspawn.strategy.PoolStrategy;

/**
 * @author Helly Guo
 * <p>
 * Created on 8/24/21 11:34 PM
 */
public class CachedPoolImpl<T extends Resettable> extends AbstractCachedPool<T> {

    private final ObjectCreator<T> creator;
    private final FetchFailStrategy fetchFailStrategy;

    public CachedPoolImpl(ObjectCreator<T> creator, int size, PoolStrategy poolStrategy) {
        super(creator, size);
        this.creator = creator;
        this.fetchFailStrategy = poolStrategy.getFetchFailStrategy();
    }

    @Override
    protected T fetchData() {
        return FetchHelper.fetchDataOrFailover(array, indexMask, walker, fetchFailStrategy, creator);
    }

}

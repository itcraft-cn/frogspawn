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

import cn.itcraft.frogeggs.impl.CachedLoopPoolImpl;
import cn.itcraft.frogeggs.impl.CachedPoolImpl;
import cn.itcraft.frogeggs.impl.PrefetchLoopPoolImpl;
import cn.itcraft.frogeggs.impl.PrefetchPoolImpl;
import cn.itcraft.frogeggs.strategy.PoolStrategy;

/**
 * @author Helly Guo
 * <p>
 * Created on 8/24/21 11:31 PM
 */
public final class ObjectsMemoryPoolFactory {

    private ObjectsMemoryPoolFactory() {
    }

    public static <T extends Resettable> ObjectsMemoryPool<T> newPool(ObjectCreator<T> creator, int size) {
        return newPool(creator, size, PoolStrategy.FETCH_FAIL_AS_NEW);
    }

    public static <T extends Resettable> ObjectsMemoryPool<T> newPool(ObjectCreator<T> creator, int size,
                                                                      PoolStrategy poolStrategy) {
        return newPool(creator, size, poolStrategy, false);
    }

    public static <T extends Resettable> ObjectsMemoryPool<T> newPool(ObjectCreator<T> creator, int size,
                                                                      PoolStrategy poolStrategy, boolean autofill) {
        if (autofill) {
            return newPrefetchPool(creator, size, poolStrategy);
        } else {
            return newNormalPool(creator, size, poolStrategy);
        }
    }

    private static <T extends Resettable> ObjectsMemoryPool<T> newPrefetchPool(ObjectCreator<T> creator, int size,
                                                                               PoolStrategy poolStrategy) {
        if (PoolStrategy.MUST_FETCH_IN_POOL.equals(poolStrategy)) {
            return new PrefetchLoopPoolImpl<>(creator, size);
        } else {
            return new PrefetchPoolImpl<>(creator, size, poolStrategy);
        }
    }

    private static <T extends Resettable> ObjectsMemoryPool<T> newNormalPool(ObjectCreator<T> creator, int size,
                                                                             PoolStrategy poolStrategy) {
        if (PoolStrategy.MUST_FETCH_IN_POOL.equals(poolStrategy)) {
            return new CachedLoopPoolImpl<>(creator, size);
        } else {
            return new CachedPoolImpl<>(creator, size, poolStrategy);
        }
    }

}

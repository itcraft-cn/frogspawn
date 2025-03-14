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
package cn.itcraft.frogeggs.impl;

import cn.itcraft.frogeggs.ObjectCreator;
import cn.itcraft.frogeggs.Resettable;
import cn.itcraft.frogeggs.constants.Constants;
import cn.itcraft.frogeggs.misc.SoftRefStore;

/**
 * @author Helly Guo
 * <p>
 * Created on 8/24/21 11:34 PM
 */
public abstract class AbstractPrefetchPoolImpl<T extends Resettable> extends AbstractCachedPool<T> {

    public AbstractPrefetchPoolImpl(ObjectCreator<T> creator, int size) {
        super(creator, size);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T fetch() {
        SoftRefStore<Resettable> softRefStore = LOCAL_QUEUE.get();
        T t = (T) softRefStore.fetch();
        if (t == null) {
            t = fetchData();
            prefetch(softRefStore);
        }
        return t;
    }

    private void prefetch(SoftRefStore<Resettable> softRefStore) {
        if (softRefStore.isEmpty()) {
            for (int i = 0; i < Constants.CACHE_CAPACITY; i++) {
                softRefStore.release(fetchData());
            }
        }
    }

    @Override
    public void release(T used) {
        used.reset();
        wrapRelease(used);
    }

}

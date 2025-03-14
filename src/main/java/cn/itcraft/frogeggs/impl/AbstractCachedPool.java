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
import cn.itcraft.frogeggs.ObjectsMemoryPool;
import cn.itcraft.frogeggs.Resettable;
import cn.itcraft.frogeggs.constants.Constants;
import cn.itcraft.frogeggs.data.WrappedResettable;
import cn.itcraft.frogeggs.misc.PaddedAtomicLong;
import cn.itcraft.frogeggs.misc.SoftRefStore;
import cn.itcraft.frogeggs.util.ArrayUtil;

/**
 * @author Helly Guo
 * <p>
 * Created on 8/24/21 11:34 PM
 */
public abstract class AbstractCachedPool<T extends Resettable> implements ObjectsMemoryPool<T> {

    protected static final ThreadLocal<SoftRefStore<Resettable>> LOCAL_QUEUE =
            ThreadLocal.withInitial(SoftRefStore::new);

    /**
     * 指针
     */
    protected final PaddedAtomicLong walker = new PaddedAtomicLong(0);
    /**
     * 核心数据
     */
    @SuppressWarnings("rawtypes")
    protected final WrappedResettable[] array;
    /**
     * 下标掩码
     */
    protected final int indexMask;

    AbstractCachedPool(ObjectCreator<T> creator, int size) {
        // 容量，是比传入的略大的2的幂次方
        int calculatedCapacity = ArrayUtil.findNextPositivePowerOfTwo(size);
        int capacity = Math.min(calculatedCapacity, Constants.MAX_CAPACITY);
        // 掩码，2的幂次方-1，用以快速计算
        indexMask = capacity - 1;
        // 生成多于实际需要的数组，跳开数据，用以消除伪共享
        array = ArrayUtil.createArray(WrappedResettable.class, capacity);
        WrappedResettable<T> wrapped;
        int paddedCapacity = ArrayUtil.BUFFER_PAD + capacity;
        for (int i = ArrayUtil.BUFFER_PAD; i < paddedCapacity; i++) {
            wrapped = new WrappedResettable<>(creator.create());
            wrapped.getObj().markId(i);
            array[i] = wrapped;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public T fetch() {
        T t = (T) LOCAL_QUEUE.get().fetch();
        if (t == null || t.isInvalid()) {
            return fetchData();
        }
        return t;
    }

    protected abstract T fetchData();

    @Override
    public void release(T used) {
        if (LOCAL_QUEUE.get().release(used)) {
            wrapRelease(used);
        }
    }

    protected void wrapRelease(T used) {
        used.reset();
        int id = used.getMarkedId();
        if (id >= 0) {
            array[id].getUsed().compareAndSet(true, false);
        }
    }

}

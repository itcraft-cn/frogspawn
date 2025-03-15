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
package cn.itcraft.frogspawn.misc;

import cn.itcraft.frogspawn.Resettable;
import cn.itcraft.frogspawn.util.ArrayUtil;

import java.lang.ref.SoftReference;

import static cn.itcraft.frogspawn.constants.Constants.CACHE_CAPACITY;
import static cn.itcraft.frogspawn.util.ArrayUtil.elementAt;
import static cn.itcraft.frogspawn.util.ArrayUtil.fillElementAt;

/**
 * @author Helly Guo
 * <p>
 * Created on 8/29/21 3:45 PM
 */
public class SoftRefStore<T extends Resettable> {
    private static final int INITIAL_CAPACITY = CACHE_CAPACITY;
    private static final int MASK = INITIAL_CAPACITY - 1;

    private final SoftReference<Object[]> softRef =
            new SoftReference<>(ArrayUtil.createArray(Object.class, INITIAL_CAPACITY));

    private int size = 0;
    private long fetchWalker = 0L;
    private long releaseWalker = 0L;

    public T fetch() {
        if (size == 0) {
            return null;
        }
        Object[] array = softRef.get();
        return array == null ? null : getFromArray(array);
    }

    private T getFromArray(Object[] array) {
        T t;
        for (int i = 0; i < INITIAL_CAPACITY; i++) {
            t = fetchArrayElement(array, i);
            if (t != null) {
                return t;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private T fetchArrayElement(Object[] array, int idx) {
        long newVal = fetchWalker + idx + 1;
        T obj = (T) elementAt(array, MASK, newVal);
        if (obj == null) {
            return null;
        }
        fillElementAt(array, MASK, newVal, null);
        fetchWalker = newVal;
        size--;
        return obj;
    }

    /**
     * @param obj obj
     * @return true - need to continue release; false - needless to continue release
     */
    public boolean release(T obj) {
        if (obj == null || size == INITIAL_CAPACITY) {
            return true;
        }
        Object[] array = softRef.get();
        return array == null || putToArray(array, obj);
    }

    private boolean putToArray(Object[] array, T obj) {
        for (int i = 0; i < INITIAL_CAPACITY; i++) {
            if (restoreArrayElement(array, obj, i)) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private boolean restoreArrayElement(Object[] array, T obj, int idx) {
        long newVal = releaseWalker + idx + 1;
        T x = (T) elementAt(array, MASK, newVal);
        if (x == null) {
            fillElementAt(array, MASK, newVal, obj);
            releaseWalker = newVal;
            size++;
            return true;
        }
        return false;
    }

    public boolean isEmpty() {
        return size == 0;
    }
}

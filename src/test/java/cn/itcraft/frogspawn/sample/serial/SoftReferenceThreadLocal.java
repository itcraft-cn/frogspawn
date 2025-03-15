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
package cn.itcraft.frogspawn.sample.serial;

import java.lang.ref.SoftReference;

/**
 * 软引用{@link ThreadLocal}，供{@link SoftReferenceThreadLocalCreator}使用<br>
 * 使用软引用的原因是：通过如此操作，此对象挂钩的真实对象仍然可被 GC 回收。且回收对本对象也无关系。一旦获取为 null，会重新创建。
 *
 * @author Helly Guo
 * <p>
 * Created on 12/13/18 4:15 PM
 * @see SoftReference
 */
public class SoftReferenceThreadLocal<T> extends ThreadLocal<SoftReference<T>> {
    private final SoftRefObjectCreator<T> objCreator;

    SoftReferenceThreadLocal(SoftRefObjectCreator<T> objCreator) {
        this.objCreator = objCreator;
    }

    @Override
    protected SoftReference<T> initialValue() {
        return createSoftRef();
    }

    private SoftReference<T> createSoftRef() {
        return new SoftReference<T>(objCreator.createObject());
    }

    /**
     * 通过抛出异常封闭，不让使用
     *
     * @return 软引用
     */
    @Override
    public SoftReference<T> get() {
        throw new RuntimeException("not implemented");
    }

    /**
     * 通过抛出异常封闭，不让使用
     *
     * @param ref 软引用
     */
    @Override
    public void set(SoftReference<T> ref) {
        throw new RuntimeException("not implemented");
    }

    /**
     * 直接获取软引用目标对象
     *
     * @return 目标对象
     */
    public T getCached() {
        SoftReference<T> ref = super.get();
        // 若引用指向的真实对象已经被 GC 回收，重新创建
        if (ref.get() == null) {
            ref = createSoftRef();
            super.set(ref);
        }
        return ref.get();
    }

    /**
     * 设置软引用目标对象
     *
     * @param obj 目标对象
     */
    public void setCached(T obj) {
        super.set(new SoftReference<T>(obj));
    }
}

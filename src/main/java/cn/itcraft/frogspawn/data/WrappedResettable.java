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
package cn.itcraft.frogspawn.data;

import cn.itcraft.frogspawn.Resettable;
import cn.itcraft.frogspawn.misc.PaddedAtomicBoolean;

/**
 * 用于包装可重置对象的容器类，提供线程安全的使用状态跟踪
 * A container class for wrapping resettable objects with thread-safe usage state tracking
 *
 * @param <X> 泛型参数，必须实现Resettable接口 
 *            Generic parameter, must implement Resettable interface
 */
public class WrappedResettable<X extends Resettable> {
    /**
     * 被包装的原始可重置对象
     * The wrapped resettable object instance
     */
    private final X obj;
    
    /**
     * 原子布尔标志位（带填充），用于线程安全地跟踪对象使用状态
     * Padded atomic boolean flag for thread-safe usage state tracking
     * 
     * 使用PaddedAtomicBoolean而非普通AtomicBoolean是为了防止伪共享(false sharing)
     * Using PaddedAtomicBoolean instead of regular AtomicBoolean prevents false sharing
     */
    private final PaddedAtomicBoolean used;

    /**
     * 构造方法，初始化包装对象
     * Constructor to initialize the wrapped object
     * 
     * @param obj 需要被包装的可重置对象实例
     *            The resettable object instance to be wrapped
     */
    public WrappedResettable(X obj) {
        this.obj = obj;
        // 初始化时对象标记为未使用状态
        // Initialize the object in unused state
        this.used = new PaddedAtomicBoolean(false);
    }

    public X getObj() {
        return obj;
    }

    public PaddedAtomicBoolean getUsed() {
        return used;
    }
}

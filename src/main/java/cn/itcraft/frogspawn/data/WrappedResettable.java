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
 * 包装数据
 *
 * @param <X> 泛型，必须实现Resettable接口
 */
public class WrappedResettable<X extends Resettable> {
    /**
     * 原始数据
     */
    private final X obj;
    /**
     * 使用标识
     */
    private final PaddedAtomicBoolean used;

    public WrappedResettable(X obj) {
        this.obj = obj;
        this.used = new PaddedAtomicBoolean(false);
    }

    public X getObj() {
        return obj;
    }

    public PaddedAtomicBoolean getUsed() {
        return used;
    }
}

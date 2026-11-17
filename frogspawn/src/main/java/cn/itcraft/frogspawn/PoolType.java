/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.itcraft.frogspawn;

/**
 * 对象池类型
 * Object pool type
 *
 * 用于统一 Heap / OffHeap / FixedOffHeap 的创建入口。
 * Used by the unified factory entry for Heap / OffHeap / FixedOffHeap.
 *
 * @author Helly Guo
 */
public enum PoolType {

    /**
     * 堆内存对象池
     * Heap memory object pool
     */
    HEAP,

    /**
     * 动态非堆内存对象池
     * Dynamic off-heap memory object pool
     */
    OFF_HEAP,

    /**
     * 固定非堆内存对象池
     * Fixed off-heap memory object pool
     */
    FIXED_OFF_HEAP
}

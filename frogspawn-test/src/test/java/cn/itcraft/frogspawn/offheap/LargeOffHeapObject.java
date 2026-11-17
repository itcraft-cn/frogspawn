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
package cn.itcraft.frogspawn.offheap;

import cn.itcraft.frogspawn.platform.MemoryAccess;
import cn.itcraft.frogspawn.platform.PlatformMemory;

/**
 * 1MB 动态非堆对象（超大对象基准）
 * 1MB dynamic off-heap object for the large-object benchmark
 *
 * 对象主体即 1MB 原生内存，不使用数组字段。
 * The object body itself is a 1MB native block; no array fields are used.
 *
 * @author Helly Guo
 */
public class LargeOffHeapObject implements PrimitiveStructure {

    public static final int PAYLOAD_BYTES = 1024 * 1024;

    private static final MemoryAccess MEMORY = PlatformMemory.ACCESS;

    private long address;

    @Override
    public long getAddress() {
        return address;
    }

    @Override
    public void setAddress(long address) {
        this.address = address;
    }

    @Override
    public int getObjectSize() {
        return PAYLOAD_BYTES;
    }

    @Override
    public int getArrayFieldCount() {
        return 0;
    }

    @Override
    public long getArrayAddress(int index) {
        throw new IndexOutOfBoundsException("no array fields");
    }

    @Override
    public void setArrayAddress(int index, long address) {
        throw new IndexOutOfBoundsException("no array fields");
    }

    @Override
    public int getArrayLength(int index) {
        throw new IndexOutOfBoundsException("no array fields");
    }

    @Override
    public void writeToMemory() {
    }

    @Override
    public void readFromMemory() {
    }

    @Override
    public boolean hasDataToWrite() {
        return false;
    }

    @Override
    public void reset() {
        // 不清 address：由池管理 / do not clear address; it is managed by the pool
    }

    @Override
    public int getMarkedId() {
        return -1;
    }

    @Override
    public void markId(int id) {
    }

    public void touch(long value) {
        MEMORY.putLong(address, value);
    }

    public long read() {
        return MEMORY.getLong(address);
    }
}

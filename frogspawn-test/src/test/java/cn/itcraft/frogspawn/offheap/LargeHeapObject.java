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

import cn.itcraft.frogspawn.Resettable;

/**
 * 1MB 堆对象（超大对象基准）
 * 1MB heap object for the large-object benchmark
 *
 * @author Helly Guo
 */
public class LargeHeapObject implements Resettable {

    public static final int PAYLOAD_BYTES = 1024 * 1024;
    public static final int PAYLOAD_LONGS = PAYLOAD_BYTES / Long.BYTES;

    private final long[] payload = new long[PAYLOAD_LONGS];

    @Override
    public void reset() {
        // 大 payload 由使用方覆盖，不在此清零；基准关注池分配/GC 与地址管理
        // The large payload is overwritten by the consumer; reset excludes the 1MB clear
    }

    @Override
    public int getMarkedId() {
        return -1;
    }

    @Override
    public void markId(int id) {
    }

    public void touch(long value) {
        payload[0] = value;
    }

    public long read() {
        return payload[0];
    }
}

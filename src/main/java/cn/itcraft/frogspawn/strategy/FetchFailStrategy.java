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
package cn.itcraft.frogspawn.strategy;

import cn.itcraft.frogspawn.ObjectCreator;
import cn.itcraft.frogspawn.Resettable;
import cn.itcraft.frogspawn.failrestore.FailRestorer;

/**
 * @author Helly Guo
 * <p>
 * Created on 8/25/21 3:02 PM
 */
public enum FetchFailStrategy implements FailRestorer {
    /**
     * 不可用，抛出异常
     */
    NOT_AVAILABLE(new FailRestorer() {
        @Override
        public <T extends Resettable> T failover(ObjectCreator<T> creator) {
            throw new RuntimeException("not available");
        }
    }),
    /**
     * 返回空
     */
    NULLABLE(new FailRestorer() {
        @Override
        public <T extends Resettable> T failover(ObjectCreator<T> creator) {
            return null;
        }
    }),
    /**
     * 直接创建
     */
    CALL_CREATOR(new FailRestorer() {
        @Override
        public <T extends Resettable> T failover(ObjectCreator<T> creator) {
            return creator.create();
        }
    });

    private final FailRestorer failRestorer;

    FetchFailStrategy(FailRestorer failRestorer) {
        this.failRestorer = failRestorer;
    }

    @Override
    public <T extends Resettable> T failover(ObjectCreator<T> creator) {
        return failRestorer.failover(creator);
    }
}

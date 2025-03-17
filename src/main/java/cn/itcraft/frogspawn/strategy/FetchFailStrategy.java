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
 * 定义在获取对象失败时的恢复策略枚举
 * <p>
 * Enum defining recovery strategies when object fetching fails
 *
 * @author Helly Guo
 * <p>
 * Created on 8/25/21 3:02 PM
 */
public enum FetchFailStrategy implements FailRestorer {
    /**
     * 不可用，抛出异常
     * Strategy: Throw exception when unavailable
     */
    NOT_AVAILABLE(new FailRestorer() {
        @Override
        public <T extends Resettable> T failover(ObjectCreator<T> creator) {
            throw new RuntimeException("not available");
        }
    }),

    /**
     * 返回空值
     * Strategy: Return null when fails
     */
    NULLABLE(new FailRestorer() {
        @Override
        public <T extends Resettable> T failover(ObjectCreator<T> creator) {
            return null;
        }
    }),

    /**
     * 直接调用创建器创建新实例
     * Strategy: Directly create new instance using creator
     */
    CALL_CREATOR(new FailRestorer() {
        @Override
        public <T extends Resettable> T failover(ObjectCreator<T> creator) {
            return creator.create();
        }
    });

    // 持有具体的失败恢复处理器
    // Holds concrete failover handler implementation
    private final FailRestorer failRestorer;

    /**
     * 枚举构造函数
     * Enum constructor
     *
     * @param failRestorer 具体的失败恢复策略实现 | Concrete failover strategy implementation
     */
    FetchFailStrategy(FailRestorer failRestorer) {
        this.failRestorer = failRestorer;
    }

    /**
     * 执行失败恢复操作
     * Execute failover operation
     *
     * @param creator 对象创建器 | Object creator
     * @return 恢复后的对象（可能为null） | Recovered object (maybe null)
     */
    @Override
    public <T extends Resettable> T failover(ObjectCreator<T> creator) {
        // 委托给具体的策略实现
        // Delegate to concrete strategy implementation
        return failRestorer.failover(creator);
    }
}

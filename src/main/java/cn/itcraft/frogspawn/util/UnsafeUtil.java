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
package cn.itcraft.frogspawn.util;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

/**
 * Unsafe 工具类 - 提供对底层非安全操作的访问
 * Unsafe utility class - Provides access to low-level unsafe operations
 * <p>
 * 注意：sun.misc.Unsafe 是JDK内部API，在不同Java版本中可能不可用或不兼容，
 * 使用此类需要特别小心并了解潜在风险
 * Note: sun.misc.Unsafe is an internal JDK API that may be unavailable or
 * incompatible in different Java versions. Use with caution and understand
 * the potential risks.
 * <p>
 * Created by Helly on 2017/05/17.
 */
public final class UnsafeUtil {
    /**
     * Unsafe 单例实例
     * Singleton instance of Unsafe
     */
    private static final Unsafe THE_UNSAFE;

    // 静态初始化块加载Unsafe实例
    // Static initializer block to load Unsafe instance
    static {
        try {
            // 定义特权操作来获取Unsafe实例
            // Define privileged action to get Unsafe instance
            final PrivilegedExceptionAction<Unsafe> action = () -> {
                // 通过反射获取Unsafe类的theUnsafe字段
                // Get theUnsafe field via reflection
                Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
                // 绕过Java语言访问检查
                // Bypass Java language access checks
                theUnsafe.setAccessible(true);
                // 获取静态字段的值（null表示静态字段）
                // Get static field value (null indicates static field)
                return (Unsafe) theUnsafe.get(null);
            };

            // 在特权块中执行敏感操作
            // Execute sensitive operation in privileged block
            THE_UNSAFE = AccessController.doPrivileged(action);
        } catch (Exception e) {
            // 将检查异常转换为运行时异常
            // Convert checked exception to runtime exception
            throw new RuntimeException("Unable to load unsafe", e);
        }
    }

    /**
     * 私有构造函数防止实例化
     * Private constructor to prevent instantiation
     */
    private UnsafeUtil() {
    }

    /**
     * 获取Unsafe单例实例
     * Get the Unsafe singleton instance
     *
     * @return sun.misc.Unsafe 实例
     * @return sun.misc.Unsafe instance
     */
    public static Unsafe getUnsafe() {
        return THE_UNSAFE;
    }
}

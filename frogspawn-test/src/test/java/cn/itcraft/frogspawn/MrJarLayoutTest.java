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

import cn.itcraft.frogspawn.misc.PaddedAtomicLong;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * MR-JAR 布局回归测试
 * Multi-Release JAR layout regression test
 *
 * 校验不同 JDK 下加载的 PaddedAtomicLong 来自正确版本目录及注解。
 * Verifies that PaddedAtomicLong is loaded from the expected version directory
 * and carries the expected annotation on different JDKs.
 *
 * @author Helly Guo
 */
class MrJarLayoutTest {

    /**
     * PaddedAtomicLong 的类文件资源路径
     */
    private static final String CLASS_RESOURCE = "PaddedAtomicLong.class";
    /**
     * JDK9+ 覆盖层路径片段
     */
    private static final String VERSIONS_9 = "META-INF/versions/9";
    /**
     * JDK8 注解
     */
    private static final String JDK8_CONTENDED = "sun.misc.Contended";
    /**
     * JDK9+ 注解
     */
    private static final String JDK9_CONTENDED = "jdk.internal.vm.annotation.Contended";
    /**
     * JDK9 主版本号
     */
    private static final int JDK9_VERSION = 9;

    @Test
    void shouldLoadVersionedClassOnJdk9OrAbove() {
        String location = PaddedAtomicLong.class.getResource(CLASS_RESOURCE).toString();
        // 仅在从 jar 加载时校验版本目录；IDE / reactor 直接加载 class 目录时跳过
        // Only validate the version directory when loaded from a jar; skip for IDE / reactor
        assumeTrue(location.startsWith("jar:"), "not loaded from a jar: " + location);
        String annotation = PaddedAtomicLong.class.getAnnotations().length == 0
                ? "none"
                : PaddedAtomicLong.class.getAnnotations()[0].annotationType().getName();

        if (currentJavaVersion() >= JDK9_VERSION) {
            assertTrue(location.contains(VERSIONS_9), location);
            assertEquals(JDK9_CONTENDED, annotation);
        } else {
            assertFalse(location.contains(VERSIONS_9), location);
            assertEquals(JDK8_CONTENDED, annotation);
        }
    }

    /**
     * 获取当前 Java 主版本号，兼容 JDK8 的 "1.8" 格式
     * Get the current Java major version, compatible with the JDK8 "1.8" format
     *
     * @return Java 主版本号 / Java major version
     */
    private static int currentJavaVersion() {
        String version = System.getProperty("java.specification.version");
        if (version.startsWith("1.")) {
            return Integer.parseInt(version.substring(2));
        }
        int dotIndex = version.indexOf('.');
        return Integer.parseInt(dotIndex < 0 ? version : version.substring(0, dotIndex));
    }
}

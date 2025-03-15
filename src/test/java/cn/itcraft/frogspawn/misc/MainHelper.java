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
package cn.itcraft.frogspawn.misc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author Helly Guo
 * <p>
 * Created on 12/8/21 11:13 PM
 */
public final class MainHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainHelper.class);

    private MainHelper() {
    }

    public static void waitForInput(String output, char wanted) {
        LOGGER.info("{}, waiting for input: char[{}]", output, wanted);
        while (true) {
            try {
                if (waitChar(wanted)) {
                    break;
                }
            } catch (IOException e) {
                //
            }
        }
    }

    private static boolean waitChar(char wanted) throws IOException {
        int ch = System.in.read();
        int avail = System.in.available();
        System.in.skip(avail);
        return ch == wanted;
    }

    public static void waitProfiler() {
        waitForInput("wait profiler", 'r');
    }

    public static void holdJvm() {
        waitForInput("hold jvm", 'q');
    }
}

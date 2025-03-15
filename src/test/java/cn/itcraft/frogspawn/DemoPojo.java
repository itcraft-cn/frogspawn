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
package cn.itcraft.frogspawn;

/**
 * @author Helly Guo
 * <p>
 * Created on 8/25/21 1:16 AM
 */
public class DemoPojo implements Resettable {

    private int val1;
    private long val2;
    private double val3;
    private String val4;

    private int allocId = -1;

    @Override
    public void reset() {
        this.val1 = 0;
        this.val2 = 0L;
        this.val3 = 0D;
        this.val4 = "";
    }

    @Override
    public int getMarkedId() {
        return allocId;
    }

    @Override
    public void markId(int id) {
        this.allocId = id;
    }

    public int getVal1() {
        return val1;
    }

    public void setVal1(int val1) {
        this.val1 = val1;
    }

    public long getVal2() {
        return val2;
    }

    public void setVal2(long val2) {
        this.val2 = val2;
    }

    public double getVal3() {
        return val3;
    }

    public void setVal3(double val3) {
        this.val3 = val3;
    }

    public String getVal4() {
        return val4;
    }

    public void setVal4(String val4) {
        this.val4 = val4;
    }

    @Override
    public String toString() {
        return "DemoPojo{" +
                "val1=" + val1 +
                ", val2=" + val2 +
                ", val3=" + val3 +
                ", val4='" + val4 + '\'' +
                '}';
    }
}

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

import cn.itcraft.frogspawn.DemoPojo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Helly Guo
 * <p>
 * Created on 12/8/21 7:13 PM
 */
public class SoftRefStoreTest {

    @Test
    public void test() {
        SoftRefStore<DemoPojo> queue = new SoftRefStore<>();
        for (int i = 0; i < 100; i++) {
            queue.release(new DemoPojo());
        }
        DemoPojo pojo1 = queue.fetch();
        DemoPojo pojo2 = queue.fetch();
        DemoPojo pojo3 = queue.fetch();
        DemoPojo pojo4 = queue.fetch();
        DemoPojo pojo5 = queue.fetch();
        Assertions.assertFalse(queue.release(pojo1)
                                       && queue.release(pojo2)
                                       && queue.release(pojo3)
                                       && queue.release(pojo4)
                                       && queue.release(pojo5));
    }

}

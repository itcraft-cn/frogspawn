package cn.itcraft.frogspawn;

import cn.itcraft.frogspawn.misc.SoftRefStoreTest;
import cn.itcraft.frogspawn.util.ArrayUtilTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * @author Helly Guo
 * <p>
 * Created on 09/01/2023 14:10
 */
@Suite
@SelectClasses({
        HeapObjectsMemoryPoolTest.class,
        HeapObjectsMemoryPool2Test.class,
        SoftRefStoreTest.class,
        ArrayUtilTest.class
})
public class FrogspawnSuite {
}

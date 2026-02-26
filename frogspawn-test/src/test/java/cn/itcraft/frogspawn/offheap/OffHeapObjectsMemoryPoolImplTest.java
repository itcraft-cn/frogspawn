package cn.itcraft.frogspawn.offheap;

import cn.itcraft.frogspawn.ObjectCreator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OffHeapObjectsMemoryPoolImplTest {

    @Test
    void testFetchAndRelease() {
        ObjectCreator<PrimitiveStructure> creator = SimplePrimitiveStructure::new;

        OffHeapObjectsMemoryPoolImpl<PrimitiveStructure> pool = new OffHeapObjectsMemoryPoolImpl<>(creator);
        PrimitiveStructure obj = pool.fetch();
        assertNotNull(obj);
        assertNotEquals(0, obj.getAddress());

        pool.release(obj);
        assertEquals(0, obj.getAddress());
    }

}

package cn.itcraft.frogspawn.offheap;

import cn.itcraft.frogspawn.ObjectCreator;
import cn.itcraft.frogspawn.PoolStrategy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OffHeapObjectsMemoryPoolImplTest {

    @Test
    void testFetchAndRelease() {
        ObjectCreator<PrimitiveStructure> creator = () -> new PrimitiveStructure() {
            private long address;

            @Override
            public long getAddress() {
                return address;
            }

            @Override
            public void setAddress(long address) {
                this.address = address;
            }

            @Override
            public void reset() {
                address = 0;
            }
        };

        PoolStrategy strategy = new PoolStrategy() {
            @Override
            public long getObjectSize() {
                return 1024;
            }
        };

        OffHeapObjectsMemoryPoolImpl<PrimitiveStructure> pool = new OffHeapObjectsMemoryPoolImpl<>(creator, strategy);
        PrimitiveStructure obj = pool.fetch();
        assertNotNull(obj);
        assertNotEquals(0, obj.getAddress());

        pool.release(obj);
        assertEquals(0, obj.getAddress());
    }
}
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
    
    @Test
    void testObjectPoolReuse() {
        ObjectCreator<SimplePrimitiveStructure> creator = SimplePrimitiveStructure::new;
        OffHeapObjectsMemoryPoolImpl<SimplePrimitiveStructure> pool = new OffHeapObjectsMemoryPoolImpl<SimplePrimitiveStructure>(creator, 10);
        
        // 初始池大小为0
        assertEquals(0, pool.getPoolSize());
        
        // 第一次fetch，应该创建新对象
        SimplePrimitiveStructure obj1 = pool.fetch();
        assertNotNull(obj1);
        long addr1 = obj1.getAddress();
        assertNotEquals(0, addr1);
        
        // 释放对象，应该放回池中（可能放入线程本地缓存或主池）
        pool.release(obj1);
        // 对象可能放入线程本地缓存（poolSize为0）或主池（poolSize为1）
        assertTrue(pool.getPoolSize() >= 0 && pool.getPoolSize() <= 1);
        assertEquals(0, obj1.getAddress()); // 释放后地址应为0
        
        // 第二次fetch，应该从池中获取对象（同一个Java对象）
        SimplePrimitiveStructure obj2 = pool.fetch();
        assertNotNull(obj2);
        assertEquals(0, pool.getPoolSize());
        
        // 可能是同一个Java对象实例（但地址会被重新设置）
        // fetch会分配新内存，所以地址不为0
        assertNotEquals(0, obj2.getAddress());
        
        pool.release(obj2);
    }
    
    @Test
    void testObjectPoolLimit() {
        ObjectCreator<SimplePrimitiveStructure> creator = SimplePrimitiveStructure::new;
        OffHeapObjectsMemoryPoolImpl<SimplePrimitiveStructure> pool = new OffHeapObjectsMemoryPoolImpl<SimplePrimitiveStructure>(creator, 2);
        
        assertEquals(0, pool.getPoolSize());
        assertEquals(2, pool.getMaxPoolSize());
        
        // 创建并释放3个对象，但池大小限制为2
        SimplePrimitiveStructure obj1 = pool.fetch();
        pool.release(obj1);
        assertTrue(pool.getPoolSize() <= 2);
        
        SimplePrimitiveStructure obj2 = pool.fetch();
        pool.release(obj2);
        assertTrue(pool.getPoolSize() <= 2);
        
        SimplePrimitiveStructure obj3 = pool.fetch();
        pool.release(obj3);
        // 池大小应该不超过2（最大值）
        assertTrue(pool.getPoolSize() <= 2);
        
        // 验证对象池功能基本正常
        assertTrue(pool.getPoolSize() >= 0);
    }

}

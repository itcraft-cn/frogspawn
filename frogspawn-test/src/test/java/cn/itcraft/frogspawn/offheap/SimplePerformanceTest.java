package cn.itcraft.frogspawn.offheap;

import cn.itcraft.frogspawn.ObjectsMemoryPool;
import cn.itcraft.frogspawn.ObjectsMemoryPoolFactory;

public class SimplePerformanceTest {
    
    public static void main(String[] args) {
        // 测试非堆内存池
        ObjectsMemoryPool<SimplePrimitiveStructure> offHeapPool = 
            ObjectsMemoryPoolFactory.newOffHeapPool(SimplePrimitiveStructure::new);
        
        // 测试堆内存池
        ObjectsMemoryPool<SimplePrimitiveStructure> heapPool = 
            ObjectsMemoryPoolFactory.newPool(SimplePrimitiveStructure::new, 3000);
        
        // 预热
        for (int i = 0; i < 10000; i++) {
            SimplePrimitiveStructure obj = offHeapPool.fetch();
            offHeapPool.release(obj);
        }
        
        for (int i = 0; i < 10000; i++) {
            SimplePrimitiveStructure obj = heapPool.fetch();
            heapPool.release(obj);
        }
        
        // 测试非堆内存池
        long startTime = System.nanoTime();
        int iterations = 1000000;
        for (int i = 0; i < iterations; i++) {
            SimplePrimitiveStructure obj = offHeapPool.fetch();
            offHeapPool.release(obj);
        }
        long endTime = System.nanoTime();
        double offHeapTime = (endTime - startTime) / 1_000_000_000.0;
        double offHeapOpsPerSec = iterations / offHeapTime;
        
        // 测试堆内存池
        startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            SimplePrimitiveStructure obj = heapPool.fetch();
            heapPool.release(obj);
        }
        endTime = System.nanoTime();
        double heapTime = (endTime - startTime) / 1_000_000_000.0;
        double heapOpsPerSec = iterations / heapTime;
        
        // 测试直接创建
        startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            SimplePrimitiveStructure obj = new SimplePrimitiveStructure();
            // 模拟使用
            obj.getAddress();
        }
        endTime = System.nanoTime();
        double newTime = (endTime - startTime) / 1_000_000_000.0;
        double newOpsPerSec = iterations / newTime;
        
        System.out.println("=== 性能测试结果 ===");
        System.out.printf("非堆内存池: %.2f ops/sec (%.2f%% of new)\n", 
            offHeapOpsPerSec, (offHeapOpsPerSec / newOpsPerSec) * 100);
        System.out.printf("堆内存池: %.2f ops/sec (%.2f%% of new)\n", 
            heapOpsPerSec, (heapOpsPerSec / newOpsPerSec) * 100);
        System.out.printf("直接创建: %.2f ops/sec (100.00%%)\n", newOpsPerSec);
        System.out.println();
        System.out.printf("非堆内存池耗时: %.6f sec\n", offHeapTime);
        System.out.printf("堆内存池耗时: %.6f sec\n", heapTime);
        System.out.printf("直接创建耗时: %.6f sec\n", newTime);
    }
}
package cn.itcraft.frogspawn.offheap;

import cn.itcraft.frogspawn.ObjectsMemoryPool;
import cn.itcraft.frogspawn.ObjectsMemoryPoolFactory;

public class FixedPoolPerformanceTest {
    
    public static void main(String[] args) {
        System.setProperty("frogspawn.cache.capacity", "64");
        
        ObjectsMemoryPool<SimpleFixedObject> fixedPool = 
            ObjectsMemoryPoolFactory.newFixedOffHeapPool(SimpleFixedObject::new, 10000, 64);
        
        ObjectsMemoryPool<SimplePrimitiveStructure> heapPool = 
            ObjectsMemoryPoolFactory.newPool(SimplePrimitiveStructure::new, 3000);
        
        for (int i = 0; i < 100000; i++) {
            SimpleFixedObject obj = fixedPool.fetch();
            fixedPool.release(obj);
        }
        
        for (int i = 0; i < 100000; i++) {
            SimplePrimitiveStructure obj = heapPool.fetch();
            heapPool.release(obj);
        }
        
        int iterations = 10000000;
        
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            SimpleFixedObject obj = fixedPool.fetch();
            fixedPool.release(obj);
        }
        long endTime = System.nanoTime();
        double fixedTime = (endTime - startTime) / 1_000_000_000.0;
        double fixedOpsPerSec = iterations / fixedTime;
        
        startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            SimplePrimitiveStructure obj = heapPool.fetch();
            heapPool.release(obj);
        }
        endTime = System.nanoTime();
        double heapTime = (endTime - startTime) / 1_000_000_000.0;
        double heapOpsPerSec = iterations / heapTime;
        
        startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            SimplePrimitiveStructure obj = new SimplePrimitiveStructure();
            obj.getAddress();
        }
        endTime = System.nanoTime();
        double newTime = (endTime - startTime) / 1_000_000_000.0;
        double newOpsPerSec = iterations / newTime;
        
        System.out.println("=== FixedOffHeapPool Performance Test ===");
        System.out.printf("FixedOffHeapPool: %.2f ops/sec (%.2f%% of new, %.2f%% of heap)\n", 
            fixedOpsPerSec, (fixedOpsPerSec / newOpsPerSec) * 100, (fixedOpsPerSec / heapOpsPerSec) * 100);
        System.out.printf("Heap Pool: %.2f ops/sec (%.2f%% of new)\n", 
            heapOpsPerSec, (heapOpsPerSec / newOpsPerSec) * 100);
        System.out.printf("new operation: %.2f ops/sec (100.00%%)\n", newOpsPerSec);
        System.out.println();
        System.out.printf("FixedOffHeapPool time: %.6f sec\n", fixedTime);
        System.out.printf("Heap Pool time: %.6f sec\n", heapTime);
        System.out.printf("new operation time: %.6f sec\n", newTime);
        System.out.println();
        double fixedOpsPerUs = fixedOpsPerSec / 1_000_000;
        double heapOpsPerUs = heapOpsPerSec / 1_000_000;
        double newOpsPerUs = newOpsPerSec / 1_000_000;
        System.out.printf("FixedOffHeapPool: %.2f M ops/sec (%.2f ops/us)\n", fixedOpsPerSec / 1_000_000, fixedOpsPerUs);
        System.out.printf("Heap Pool: %.2f M ops/sec (%.2f ops/us)\n", heapOpsPerSec / 1_000_000, heapOpsPerUs);
        System.out.printf("new operation: %.2f M ops/sec (%.2f ops/us)\n", newOpsPerSec / 1_000_000, newOpsPerUs);
        
        System.out.println("\n=== Round 2 ===");
        runTest(fixedPool, heapPool, iterations);
        
        System.out.println("\n=== Round 3 ===");
        runTest(fixedPool, heapPool, iterations);
        
        System.out.println("\n=== Round 4 ===");
        runTest(fixedPool, heapPool, iterations);
        
        System.out.println("\n=== Round 5 ===");
        runTest(fixedPool, heapPool, iterations);
    }
    
    private static void runTest(ObjectsMemoryPool<SimpleFixedObject> fixedPool, 
                                ObjectsMemoryPool<SimplePrimitiveStructure> heapPool, 
                                int iterations) {
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            SimpleFixedObject obj = fixedPool.fetch();
            fixedPool.release(obj);
        }
        long endTime = System.nanoTime();
        double fixedTime = (endTime - startTime) / 1_000_000_000.0;
        double fixedOpsPerSec = iterations / fixedTime;
        
        startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            SimplePrimitiveStructure obj = heapPool.fetch();
            heapPool.release(obj);
        }
        endTime = System.nanoTime();
        double heapTime = (endTime - startTime) / 1_000_000_000.0;
        double heapOpsPerSec = iterations / heapTime;
        
        startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            SimplePrimitiveStructure obj = new SimplePrimitiveStructure();
            obj.getAddress();
        }
        endTime = System.nanoTime();
        double newTime = (endTime - startTime) / 1_000_000_000.0;
        double newOpsPerSec = iterations / newTime;
        
        System.out.printf("FixedOffHeapPool: %.2f ops/us (%.1f%% of heap)\n", 
            fixedOpsPerSec / 1_000_000, (fixedOpsPerSec / heapOpsPerSec) * 100);
        System.out.printf("Heap Pool: %.2f ops/us\n", heapOpsPerSec / 1_000_000);
        System.out.printf("new operation: %.2f ops/us\n", newOpsPerSec / 1_000_000);
    }
}
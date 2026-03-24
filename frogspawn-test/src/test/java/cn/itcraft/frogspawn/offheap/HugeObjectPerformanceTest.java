package cn.itcraft.frogspawn.offheap;

import cn.itcraft.frogspawn.ObjectsMemoryPool;
import cn.itcraft.frogspawn.ObjectsMemoryPoolFactory;

/**
 * 大型复杂对象性能测试
 * Huge complex object performance test
 * 
 * 对比三种方案：
 * 1. Heap Pool - 堆内存池
 * 2. FixedOffHeapPool - 固定非堆内存池
 * 3. OffHeap Pool - 原始非堆内存池
 * 4. new - 直接创建
 *
 * @author Helly Guo
 * Created on 2026-03-24
 */
public class HugeObjectPerformanceTest {
    
    public static void main(String[] args) {
        System.setProperty("frogspawn.cache.capacity", "64");
        
        ObjectsMemoryPool<HugeFixedOffHeapObject> fixedPool = 
            ObjectsMemoryPoolFactory.newFixedOffHeapPool(
                HugeFixedOffHeapObject::new, 10000, HugeFixedOffHeapObject.OBJECT_SIZE);
        
        ObjectsMemoryPool<HugeHeapObject> heapPool = 
            ObjectsMemoryPoolFactory.newPool(HugeHeapObject::new, 3000);
        
        System.out.println("=== HugeComplexObject Performance Test ===");
        System.out.println("Object size: ~2KB (25 fields + 3 arrays)");
        System.out.println();
        
        int warmupIterations = 100000;
        int testIterations = 1000000;
        
        System.out.println("Warmup phase...");
        
        for (int i = 0; i < warmupIterations; i++) {
            HugeFixedOffHeapObject obj = fixedPool.fetch();
            fixedPool.release(obj);
        }
        
        for (int i = 0; i < warmupIterations; i++) {
            HugeHeapObject obj = heapPool.fetch();
            heapPool.release(obj);
        }
        
        for (int i = 0; i < warmupIterations; i++) {
            new HugeHeapObject();
        }
        
        System.out.println("Running benchmark...\n");
        
        for (int round = 1; round <= 5; round++) {
            System.out.println("=== Round " + round + " ===");
            
            double fixedOps = testFixedPool(fixedPool, testIterations);
            double heapOps = testHeapPool(heapPool, testIterations);
            double newOps = testNew(testIterations);
            
            System.out.printf("FixedOffHeapPool: %6.2f ops/us (%5.1f%% of heap)\n", 
                fixedOps / 1_000_000, (fixedOps / heapOps) * 100);
            System.out.printf("Heap Pool:        %6.2f ops/us (%5.1f%% of new)\n", 
                heapOps / 1_000_000, (heapOps / newOps) * 100);
            System.out.printf("new operation:    %6.2f ops/us (100.0%%)\n", 
                newOps / 1_000_000);
            System.out.println();
        }
        
        System.out.println("=== Memory Usage Comparison ===");
        System.out.println("Heap Object:        ~2KB per object (JVM heap, GC managed)");
        System.out.println("FixedOffHeapObject:  2KB per object (direct memory, no GC for data)");
        System.out.println("OffHeapObject:       ~2KB per object (direct memory, requires writeToMemory/readFromMemory)");
        System.out.println();
        System.out.println("=== Key Differences ===");
        System.out.println("Heap Pool:        Best for general use, simple API, GC pressure");
        System.out.println("FixedOffHeapPool: Zero GC for pooled data, best off-heap performance");
        System.out.println("OffHeap Pool:     Most flexible, but requires explicit memory management");
    }
    
    private static double testFixedPool(ObjectsMemoryPool<HugeFixedOffHeapObject> pool, int iterations) {
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            HugeFixedOffHeapObject obj = pool.fetch();
            pool.release(obj);
        }
        long endTime = System.nanoTime();
        return iterations / ((endTime - startTime) / 1_000_000_000.0);
    }
    
    private static double testHeapPool(ObjectsMemoryPool<HugeHeapObject> pool, int iterations) {
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            HugeHeapObject obj = pool.fetch();
            pool.release(obj);
        }
        long endTime = System.nanoTime();
        return iterations / ((endTime - startTime) / 1_000_000_000.0);
    }
    
    private static double testOffHeapPool(ObjectsMemoryPool<HugeOffHeapObject> pool, int iterations) {
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            HugeOffHeapObject obj = pool.fetch();
            obj.setData(i, i + 1, i * 2, i * 3, i * 0.5);
            pool.release(obj);
        }
        long endTime = System.nanoTime();
        return iterations / ((endTime - startTime) / 1_000_000_000.0);
    }
    
    private static double testNew(int iterations) {
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            new HugeHeapObject();
        }
        long endTime = System.nanoTime();
        return iterations / ((endTime - startTime) / 1_000_000_000.0);
    }
}
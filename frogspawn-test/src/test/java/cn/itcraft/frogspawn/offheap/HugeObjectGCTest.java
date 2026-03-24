package cn.itcraft.frogspawn.offheap;

import cn.itcraft.frogspawn.ObjectsMemoryPool;
import cn.itcraft.frogspawn.ObjectsMemoryPoolFactory;

/**
 * 大型复杂对象 GC 压力测试
 * Huge complex object GC pressure test
 * 
 * 对比大量对象创建对 GC 的影响
 *
 * @author Helly Guo
 * Created on 2026-03-24
 */
public class HugeObjectGCTest {
    
    private static final int OBJECTS_PER_ITERATION = 10000;
    private static final int ITERATIONS = 100;
    
    public static void main(String[] args) {
        System.setProperty("frogspawn.cache.capacity", "64");
        
        ObjectsMemoryPool<HugeFixedOffHeapObject> fixedPool = 
            ObjectsMemoryPoolFactory.newFixedOffHeapPool(
                HugeFixedOffHeapObject::new, OBJECTS_PER_ITERATION, HugeFixedOffHeapObject.OBJECT_SIZE);
        
        System.out.println("=== HugeComplexObject GC Pressure Test ===");
        System.out.println("Objects per iteration: " + OBJECTS_PER_ITERATION);
        System.out.println("Object size: ~2KB");
        System.out.println("Total iterations: " + ITERATIONS);
        System.out.println();
        
        System.out.println("=== Test 1: Direct new (expect GC pressure) ===");
        long totalGcCount = getGcCount();
        long startTime = System.nanoTime();
        
        for (int iter = 0; iter < ITERATIONS; iter++) {
            HugeHeapObject[] objects = new HugeHeapObject[OBJECTS_PER_ITERATION];
            for (int i = 0; i < OBJECTS_PER_ITERATION; i++) {
                objects[i] = new HugeHeapObject();
                objects[i].setData(i, i + 1, i * 2, i * 3, i * 0.5);
            }
        }
        
        long endTime = System.nanoTime();
        long gcCount = getGcCount() - totalGcCount;
        double time1 = (endTime - startTime) / 1_000_000.0;
        
        System.out.printf("Time: %.2f ms, GC count: %d\n", time1, gcCount);
        System.out.println();
        
        System.out.println("=== Test 2: Heap Pool (GC for pooled objects) ===");
        totalGcCount = getGcCount();
        startTime = System.nanoTime();
        
        ObjectsMemoryPool<HugeHeapObject> heapPool = 
            ObjectsMemoryPoolFactory.newPool(HugeHeapObject::new, OBJECTS_PER_ITERATION);
        
        for (int iter = 0; iter < ITERATIONS; iter++) {
            HugeHeapObject[] objects = new HugeHeapObject[OBJECTS_PER_ITERATION];
            for (int i = 0; i < OBJECTS_PER_ITERATION; i++) {
                objects[i] = heapPool.fetch();
                objects[i].setData(i, i + 1, i * 2, i * 3, i * 0.5);
            }
            for (int i = 0; i < OBJECTS_PER_ITERATION; i++) {
                heapPool.release(objects[i]);
            }
        }
        
        endTime = System.nanoTime();
        gcCount = getGcCount() - totalGcCount;
        double time2 = (endTime - startTime) / 1_000_000.0;
        
        System.out.printf("Time: %.2f ms, GC count: %d\n", time2, gcCount);
        System.out.println();
        
        System.out.println("=== Test 3: FixedOffHeap Pool (zero GC for pooled data) ===");
        totalGcCount = getGcCount();
        startTime = System.nanoTime();
        
        HugeFixedOffHeapObject[] objects = new HugeFixedOffHeapObject[OBJECTS_PER_ITERATION];
        for (int iter = 0; iter < ITERATIONS; iter++) {
            for (int i = 0; i < OBJECTS_PER_ITERATION; i++) {
                objects[i] = fixedPool.fetch();
                objects[i].setData(i, i + 1, i * 2, i * 3, i * 0.5);
            }
            for (int i = 0; i < OBJECTS_PER_ITERATION; i++) {
                fixedPool.release(objects[i]);
            }
        }
        
        endTime = System.nanoTime();
        gcCount = getGcCount() - totalGcCount;
        double time3 = (endTime - startTime) / 1_000_000.0;
        
        System.out.printf("Time: %.2f ms, GC count: %d\n", time3, gcCount);
        System.out.println();
        
        System.out.println("=== Summary ===");
        System.out.printf("Direct new:         %.2f ms\n", time1);
        System.out.printf("Heap Pool:          %.2f ms (%.1f%% of new)\n", time2, (time2 / time1) * 100);
        System.out.printf("FixedOffHeap Pool:  %.2f ms (%.1f%% of new)\n", time3, (time3 / time1) * 100);
        System.out.println();
        System.out.println("FixedOffHeap Pool advantages:");
        System.out.println("- Zero GC for pooled data (2KB * 10000 = 20MB off-heap)");
        System.out.println("- Predictable memory usage");
        System.out.println("- No GC pauses from pooled objects");
    }
    
    private static long getGcCount() {
        long total = 0;
        for (java.lang.management.GarbageCollectorMXBean gc : 
             java.lang.management.ManagementFactory.getGarbageCollectorMXBeans()) {
            if (gc.getCollectionCount() != -1) {
                total += gc.getCollectionCount();
            }
        }
        return total;
    }
}
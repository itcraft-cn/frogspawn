package cn.itcraft.frogspawn.offheap;

import cn.itcraft.frogspawn.ObjectsMemoryPool;
import cn.itcraft.frogspawn.ObjectsMemoryPoolFactory;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;

/**
 * 统一口径性能测试
 * Unified performance test
 *
 * 测试维度：
 * 1. 纯池操作 (fetch+release) - 测池本身开销
 * 2. 带数据操作 (fetch+setData+release) - 测真实场景
 * 3. GC压力测试 (批量循环) - 测长时间运行GC影响
 *
 * @author Helly Guo
 * Created on 2026-03-24
 */
public class UnifiedPerformanceTest {
    
    private static final int WARMUP_ITERATIONS = 100000;
    private static final int TEST_ITERATIONS = 1000000;
    private static final int GC_TEST_OBJECTS = 10000;
    private static final int GC_TEST_ITERATIONS = 100;
    
    public static void main(String[] args) {
        System.setProperty("frogspawn.cache.capacity", "64");
        
        System.out.println("========================================");
        System.out.println("  Frogspawn Performance Test (Unified)");
        System.out.println("========================================");
        System.out.println();
        
        System.out.println("Test Configuration:");
        System.out.println("  Warmup iterations: " + WARMUP_ITERATIONS);
        System.out.println("  Test iterations: " + TEST_ITERATIONS);
        System.out.println("  GC test: " + GC_TEST_OBJECTS + " objects x " + GC_TEST_ITERATIONS + " iterations");
        System.out.println();
        
        testSmallObjects();
        System.out.println();
        testLargeObjects();
    }
    
    @SuppressWarnings("unchecked")
    private static void testSmallObjects() {
        System.out.println("========================================");
        System.out.println("  Small Objects (~64 bytes)");
        System.out.println("========================================");
        System.out.println();
        
        ObjectsMemoryPool<SimpleFixedObject> fixedPool = 
            ObjectsMemoryPoolFactory.newFixedOffHeapPool(SimpleFixedObject::new, 10000, 64);
        
        ObjectsMemoryPool<SimplePrimitiveStructure> heapPool = 
            ObjectsMemoryPoolFactory.newPool(SimplePrimitiveStructure::new, 3000);
        
        warmupSmall(fixedPool, heapPool);
        
        System.out.println("--- Test 1: Pool Operations Only (fetch+release) ---");
        System.out.printf("%-20s %12s %8s%n", "Implementation", "TPS (ops/us)", "GC Count");
        System.out.println("-".repeat(42));
        
        TestResult fixedResult1 = testSmallFixedOpsOnly(fixedPool, TEST_ITERATIONS);
        TestResult heapResult1 = testSmallHeapOpsOnly(heapPool, TEST_ITERATIONS);
        
        System.out.printf("%-20s %12.2f %8d%n", "FixedOffHeapPool", fixedResult1.tps / 1_000_000, fixedResult1.gcCount);
        System.out.printf("%-20s %12.2f %8d%n", "HeapPool", heapResult1.tps / 1_000_000, heapResult1.gcCount);
        System.out.printf("%-20s %12.1f%%%n", "FixedOffHeap%", (fixedResult1.tps / heapResult1.tps) * 100);
        System.out.println();
        
        System.out.println("--- Test 2: With Data Operations (fetch+setData+release) ---");
        System.out.printf("%-20s %12s %8s%n", "Implementation", "TPS (ops/us)", "GC Count");
        System.out.println("-".repeat(42));
        
        TestResult fixedResult2 = testSmallFixedWithData(fixedPool, TEST_ITERATIONS);
        TestResult heapResult2 = testSmallHeapWithData(heapPool, TEST_ITERATIONS);
        TestResult newResult = testSmallNewOperation(TEST_ITERATIONS);
        
        System.out.printf("%-20s %12.2f %8d%n", "FixedOffHeapPool", fixedResult2.tps / 1_000_000, fixedResult2.gcCount);
        System.out.printf("%-20s %12.2f %8d%n", "HeapPool", heapResult2.tps / 1_000_000, heapResult2.gcCount);
        System.out.printf("%-20s %12.2f %8d%n", "new", newResult.tps / 1_000_000, newResult.gcCount);
        System.out.printf("%-20s %12.1f%%%n", "FixedOffHeap%", (fixedResult2.tps / heapResult2.tps) * 100);
        System.out.printf("%-20s %12.1f%%%n", "HeapPool%ofNew", (heapResult2.tps / newResult.tps) * 100);
        System.out.println();
        
        System.out.println("--- Test 3: GC Pressure Test (batch cycles) ---");
        System.out.printf("%-20s %12s %8s%n", "Implementation", "Time (ms)", "GC Count");
        System.out.println("-".repeat(42));
        
        GcTestResult fixedGc = testSmallGcFixed(fixedPool, GC_TEST_OBJECTS, GC_TEST_ITERATIONS);
        GcTestResult heapGc = testSmallGcHeap(heapPool, GC_TEST_OBJECTS, GC_TEST_ITERATIONS);
        GcTestResult newGc = testSmallGcNew(GC_TEST_OBJECTS, GC_TEST_ITERATIONS);
        
        System.out.printf("%-20s %12.2f %8d%n", "FixedOffHeapPool", fixedGc.timeMs, fixedGc.gcCount);
        System.out.printf("%-20s %12.2f %8d%n", "HeapPool", heapGc.timeMs, heapGc.gcCount);
        System.out.printf("%-20s %12.2f %8d%n", "new", newGc.timeMs, newGc.gcCount);
        System.out.printf("%-20s %12.1f%%%n", "FixedOffHeap%", (fixedGc.timeMs / heapGc.timeMs) * 100);
        System.out.printf("%-20s %12.1f%%%n", "HeapPool%ofNew", (heapGc.timeMs / newGc.timeMs) * 100);
    }
    
    @SuppressWarnings("unchecked")
    private static void testLargeObjects() {
        System.out.println();
        System.out.println("========================================");
        System.out.println("  Large Objects (~2KB)");
        System.out.println("========================================");
        System.out.println();
        
        ObjectsMemoryPool<HugeFixedOffHeapObject> fixedPool = 
            ObjectsMemoryPoolFactory.newFixedOffHeapPool(
                HugeFixedOffHeapObject::new, GC_TEST_OBJECTS, HugeFixedOffHeapObject.OBJECT_SIZE);
        
        ObjectsMemoryPool<HugeHeapObject> heapPool = 
            ObjectsMemoryPoolFactory.newPool(HugeHeapObject::new, GC_TEST_OBJECTS);
        
        warmupLarge(fixedPool, heapPool);
        
        System.out.println("--- Test 1: Pool Operations Only (fetch+release) ---");
        System.out.printf("%-20s %12s %8s%n", "Implementation", "TPS (ops/us)", "GC Count");
        System.out.println("-".repeat(42));
        
        TestResult fixedResult1 = testLargeFixedOpsOnly(fixedPool, TEST_ITERATIONS);
        TestResult heapResult1 = testLargeHeapOpsOnly(heapPool, TEST_ITERATIONS);
        
        System.out.printf("%-20s %12.2f %8d%n", "FixedOffHeapPool", fixedResult1.tps / 1_000_000, fixedResult1.gcCount);
        System.out.printf("%-20s %12.2f %8d%n", "HeapPool", heapResult1.tps / 1_000_000, heapResult1.gcCount);
        System.out.printf("%-20s %12.1f%%%n", "FixedOffHeap%", (fixedResult1.tps / heapResult1.tps) * 100);
        System.out.println();
        
        System.out.println("--- Test 2: With Data Operations (fetch+setData+release) ---");
        System.out.printf("%-20s %12s %8s%n", "Implementation", "TPS (ops/us)", "GC Count");
        System.out.println("-".repeat(42));
        
        TestResult fixedResult2 = testLargeFixedWithData(fixedPool, TEST_ITERATIONS);
        TestResult heapResult2 = testLargeHeapWithData(heapPool, TEST_ITERATIONS);
        TestResult newResult = testLargeNewOperation(TEST_ITERATIONS);
        
        System.out.printf("%-20s %12.2f %8d%n", "FixedOffHeapPool", fixedResult2.tps / 1_000_000, fixedResult2.gcCount);
        System.out.printf("%-20s %12.2f %8d%n", "HeapPool", heapResult2.tps / 1_000_000, heapResult2.gcCount);
        System.out.printf("%-20s %12.2f %8d%n", "new", newResult.tps / 1_000_000, newResult.gcCount);
        System.out.printf("%-20s %12.1f%%%n", "FixedOffHeap%", (fixedResult2.tps / heapResult2.tps) * 100);
        System.out.printf("%-20s %12.1f%%%n", "HeapPool%ofNew", (heapResult2.tps / newResult.tps) * 100);
        System.out.println();
        
        System.out.println("--- Test 3: GC Pressure Test (batch cycles) ---");
        System.out.printf("%-20s %12s %8s%n", "Implementation", "Time (ms)", "GC Count");
        System.out.println("-".repeat(42));
        
        GcTestResult fixedGc = testLargeGcFixed(fixedPool, GC_TEST_OBJECTS, GC_TEST_ITERATIONS);
        GcTestResult heapGc = testLargeGcHeap(heapPool, GC_TEST_OBJECTS, GC_TEST_ITERATIONS);
        GcTestResult newGc = testLargeGcNew(GC_TEST_OBJECTS, GC_TEST_ITERATIONS);
        
        System.out.printf("%-20s %12.2f %8d%n", "FixedOffHeapPool", fixedGc.timeMs, fixedGc.gcCount);
        System.out.printf("%-20s %12.2f %8d%n", "HeapPool", heapGc.timeMs, heapGc.gcCount);
        System.out.printf("%-20s %12.2f %8d%n", "new", newGc.timeMs, newGc.gcCount);
        System.out.printf("%-20s %12.1f%%%n", "FixedOffHeap%", (fixedGc.timeMs / heapGc.timeMs) * 100);
        System.out.printf("%-20s %12.1f%%%n", "HeapPool%ofNew", (heapGc.timeMs / newGc.timeMs) * 100);
    }
    
    // ========== Small Object Tests ==========
    
    private static void warmupSmall(ObjectsMemoryPool<SimpleFixedObject> fixedPool, 
                                    ObjectsMemoryPool<SimplePrimitiveStructure> heapPool) {
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            SimpleFixedObject obj = fixedPool.fetch();
            fixedPool.release(obj);
        }
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            SimplePrimitiveStructure obj = heapPool.fetch();
            heapPool.release(obj);
        }
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            new SimplePrimitiveStructure();
        }
    }
    
    private static TestResult testSmallFixedOpsOnly(ObjectsMemoryPool<SimpleFixedObject> pool, int iterations) {
        long gcBefore = getGcCount();
        long startTime = System.nanoTime();
        
        for (int i = 0; i < iterations; i++) {
            SimpleFixedObject obj = pool.fetch();
            pool.release(obj);
        }
        
        long endTime = System.nanoTime();
        long gcAfter = getGcCount();
        
        double tps = iterations / ((endTime - startTime) / 1_000_000_000.0);
        return new TestResult(tps, (int)(gcAfter - gcBefore));
    }
    
    private static TestResult testSmallHeapOpsOnly(ObjectsMemoryPool<SimplePrimitiveStructure> pool, int iterations) {
        long gcBefore = getGcCount();
        long startTime = System.nanoTime();
        
        for (int i = 0; i < iterations; i++) {
            SimplePrimitiveStructure obj = pool.fetch();
            pool.release(obj);
        }
        
        long endTime = System.nanoTime();
        long gcAfter = getGcCount();
        
        double tps = iterations / ((endTime - startTime) / 1_000_000_000.0);
        return new TestResult(tps, (int)(gcAfter - gcBefore));
    }
    
    private static TestResult testSmallFixedWithData(ObjectsMemoryPool<SimpleFixedObject> pool, int iterations) {
        long gcBefore = getGcCount();
        long startTime = System.nanoTime();
        
        for (int i = 0; i < iterations; i++) {
            SimpleFixedObject obj = pool.fetch();
            pool.release(obj);
        }
        
        long endTime = System.nanoTime();
        long gcAfter = getGcCount();
        
        double tps = iterations / ((endTime - startTime) / 1_000_000_000.0);
        return new TestResult(tps, (int)(gcAfter - gcBefore));
    }
    
    private static TestResult testSmallHeapWithData(ObjectsMemoryPool<SimplePrimitiveStructure> pool, int iterations) {
        long gcBefore = getGcCount();
        long startTime = System.nanoTime();
        
        for (int i = 0; i < iterations; i++) {
            SimplePrimitiveStructure obj = pool.fetch();
            pool.release(obj);
        }
        
        long endTime = System.nanoTime();
        long gcAfter = getGcCount();
        
        double tps = iterations / ((endTime - startTime) / 1_000_000_000.0);
        return new TestResult(tps, (int)(gcAfter - gcBefore));
    }
    
    private static TestResult testSmallNewOperation(int iterations) {
        long gcBefore = getGcCount();
        long startTime = System.nanoTime();
        
        for (int i = 0; i < iterations; i++) {
            new SimplePrimitiveStructure();
        }
        
        long endTime = System.nanoTime();
        long gcAfter = getGcCount();
        
        double tps = iterations / ((endTime - startTime) / 1_000_000_000.0);
        return new TestResult(tps, (int)(gcAfter - gcBefore));
    }
    
    private static GcTestResult testSmallGcFixed(ObjectsMemoryPool<SimpleFixedObject> pool, int objects, int iterations) {
        SimpleFixedObject[] objArray = new SimpleFixedObject[objects];
        
        // 预热一轮
        for (int i = 0; i < objects; i++) {
            objArray[i] = pool.fetch();
        }
        for (int i = 0; i < objects; i++) {
            pool.release(objArray[i]);
        }
        
        long gcBefore = getGcCount();
        long startTime = System.nanoTime();
        
        for (int iter = 0; iter < iterations; iter++) {
            for (int i = 0; i < objects; i++) {
                objArray[i] = pool.fetch();
            }
            for (int i = 0; i < objects; i++) {
                pool.release(objArray[i]);
            }
        }
        
        long endTime = System.nanoTime();
        long gcAfter = getGcCount();
        
        return new GcTestResult((endTime - startTime) / 1_000_000.0, (int)(gcAfter - gcBefore));
    }
    
    private static GcTestResult testSmallGcHeap(ObjectsMemoryPool<SimplePrimitiveStructure> pool, int objects, int iterations) {
        SimplePrimitiveStructure[] objArray = new SimplePrimitiveStructure[objects];
        
        // 预热一轮
        for (int i = 0; i < objects; i++) {
            objArray[i] = pool.fetch();
        }
        for (int i = 0; i < objects; i++) {
            pool.release(objArray[i]);
        }
        
        long gcBefore = getGcCount();
        long startTime = System.nanoTime();
        
        for (int iter = 0; iter < iterations; iter++) {
            for (int i = 0; i < objects; i++) {
                objArray[i] = pool.fetch();
            }
            for (int i = 0; i < objects; i++) {
                pool.release(objArray[i]);
            }
        }
        
        long endTime = System.nanoTime();
        long gcAfter = getGcCount();
        
        return new GcTestResult((endTime - startTime) / 1_000_000.0, (int)(gcAfter - gcBefore));
    }
    
    private static GcTestResult testSmallGcNew(int objects, int iterations) {
        SimplePrimitiveStructure[] objArray = new SimplePrimitiveStructure[objects];
        
        // 预热一轮
        for (int i = 0; i < objects; i++) {
            objArray[i] = new SimplePrimitiveStructure();
        }
        
        long gcBefore = getGcCount();
        long startTime = System.nanoTime();
        
        for (int iter = 0; iter < iterations; iter++) {
            for (int i = 0; i < objects; i++) {
                objArray[i] = new SimplePrimitiveStructure();
            }
        }
        
        long endTime = System.nanoTime();
        long gcAfter = getGcCount();
        
        return new GcTestResult((endTime - startTime) / 1_000_000.0, (int)(gcAfter - gcBefore));
    }
    
    // ========== Large Object Tests ==========
    
    private static void warmupLarge(ObjectsMemoryPool<HugeFixedOffHeapObject> fixedPool, 
                                    ObjectsMemoryPool<HugeHeapObject> heapPool) {
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            HugeFixedOffHeapObject obj = fixedPool.fetch();
            fixedPool.release(obj);
        }
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            HugeHeapObject obj = heapPool.fetch();
            heapPool.release(obj);
        }
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            new HugeHeapObject();
        }
    }
    
    private static TestResult testLargeFixedOpsOnly(ObjectsMemoryPool<HugeFixedOffHeapObject> pool, int iterations) {
        long gcBefore = getGcCount();
        long startTime = System.nanoTime();
        
        for (int i = 0; i < iterations; i++) {
            HugeFixedOffHeapObject obj = pool.fetch();
            pool.release(obj);
        }
        
        long endTime = System.nanoTime();
        long gcAfter = getGcCount();
        
        double tps = iterations / ((endTime - startTime) / 1_000_000_000.0);
        return new TestResult(tps, (int)(gcAfter - gcBefore));
    }
    
    private static TestResult testLargeHeapOpsOnly(ObjectsMemoryPool<HugeHeapObject> pool, int iterations) {
        long gcBefore = getGcCount();
        long startTime = System.nanoTime();
        
        for (int i = 0; i < iterations; i++) {
            HugeHeapObject obj = pool.fetch();
            pool.release(obj);
        }
        
        long endTime = System.nanoTime();
        long gcAfter = getGcCount();
        
        double tps = iterations / ((endTime - startTime) / 1_000_000_000.0);
        return new TestResult(tps, (int)(gcAfter - gcBefore));
    }
    
    private static TestResult testLargeFixedWithData(ObjectsMemoryPool<HugeFixedOffHeapObject> pool, int iterations) {
        long gcBefore = getGcCount();
        long startTime = System.nanoTime();
        
        for (int i = 0; i < iterations; i++) {
            HugeFixedOffHeapObject obj = pool.fetch();
            obj.setData(i, i + 1, i * 2, i * 3, i * 0.5);
            pool.release(obj);
        }
        
        long endTime = System.nanoTime();
        long gcAfter = getGcCount();
        
        double tps = iterations / ((endTime - startTime) / 1_000_000_000.0);
        return new TestResult(tps, (int)(gcAfter - gcBefore));
    }
    
    private static TestResult testLargeHeapWithData(ObjectsMemoryPool<HugeHeapObject> pool, int iterations) {
        long gcBefore = getGcCount();
        long startTime = System.nanoTime();
        
        for (int i = 0; i < iterations; i++) {
            HugeHeapObject obj = pool.fetch();
            obj.setData(i, i + 1, i * 2, i * 3, i * 0.5);
            pool.release(obj);
        }
        
        long endTime = System.nanoTime();
        long gcAfter = getGcCount();
        
        double tps = iterations / ((endTime - startTime) / 1_000_000_000.0);
        return new TestResult(tps, (int)(gcAfter - gcBefore));
    }
    
    private static TestResult testLargeNewOperation(int iterations) {
        long gcBefore = getGcCount();
        long startTime = System.nanoTime();
        
        for (int i = 0; i < iterations; i++) {
            HugeHeapObject obj = new HugeHeapObject();
            obj.setData(i, i + 1, i * 2, i * 3, i * 0.5);
        }
        
        long endTime = System.nanoTime();
        long gcAfter = getGcCount();
        
        double tps = iterations / ((endTime - startTime) / 1_000_000_000.0);
        return new TestResult(tps, (int)(gcAfter - gcBefore));
    }
    
    private static GcTestResult testLargeGcFixed(ObjectsMemoryPool<HugeFixedOffHeapObject> pool, int objects, int iterations) {
        HugeFixedOffHeapObject[] objArray = new HugeFixedOffHeapObject[objects];
        
        // 预热一轮
        for (int i = 0; i < objects; i++) {
            objArray[i] = pool.fetch();
            objArray[i].setData(i, i + 1, i * 2, i * 3, i * 0.5);
        }
        for (int i = 0; i < objects; i++) {
            pool.release(objArray[i]);
        }
        
        long gcBefore = getGcCount();
        long startTime = System.nanoTime();
        
        for (int iter = 0; iter < iterations; iter++) {
            for (int i = 0; i < objects; i++) {
                objArray[i] = pool.fetch();
                objArray[i].setData(i, i + 1, i * 2, i * 3, i * 0.5);
            }
            for (int i = 0; i < objects; i++) {
                pool.release(objArray[i]);
            }
        }
        
        long endTime = System.nanoTime();
        long gcAfter = getGcCount();
        
        return new GcTestResult((endTime - startTime) / 1_000_000.0, (int)(gcAfter - gcBefore));
    }
    
    private static GcTestResult testLargeGcHeap(ObjectsMemoryPool<HugeHeapObject> pool, int objects, int iterations) {
        HugeHeapObject[] objArray = new HugeHeapObject[objects];
        
        // 预热一轮
        for (int i = 0; i < objects; i++) {
            objArray[i] = pool.fetch();
            objArray[i].setData(i, i + 1, i * 2, i * 3, i * 0.5);
        }
        for (int i = 0; i < objects; i++) {
            pool.release(objArray[i]);
        }
        
        long gcBefore = getGcCount();
        long startTime = System.nanoTime();
        
        for (int iter = 0; iter < iterations; iter++) {
            for (int i = 0; i < objects; i++) {
                objArray[i] = pool.fetch();
                objArray[i].setData(i, i + 1, i * 2, i * 3, i * 0.5);
            }
            for (int i = 0; i < objects; i++) {
                pool.release(objArray[i]);
            }
        }
        
        long endTime = System.nanoTime();
        long gcAfter = getGcCount();
        
        return new GcTestResult((endTime - startTime) / 1_000_000.0, (int)(gcAfter - gcBefore));
    }
    
    private static GcTestResult testLargeGcNew(int objects, int iterations) {
        HugeHeapObject[] objArray = new HugeHeapObject[objects];
        
        // 预热一轮
        for (int i = 0; i < objects; i++) {
            objArray[i] = new HugeHeapObject();
            objArray[i].setData(i, i + 1, i * 2, i * 3, i * 0.5);
        }
        
        long gcBefore = getGcCount();
        long startTime = System.nanoTime();
        
        for (int iter = 0; iter < iterations; iter++) {
            for (int i = 0; i < objects; i++) {
                objArray[i] = new HugeHeapObject();
                objArray[i].setData(i, i + 1, i * 2, i * 3, i * 0.5);
            }
        }
        
        long endTime = System.nanoTime();
        long gcAfter = getGcCount();
        
        return new GcTestResult((endTime - startTime) / 1_000_000.0, (int)(gcAfter - gcBefore));
    }
    
    // ========== Utilities ==========
    
    private static long getGcCount() {
        long total = 0;
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean gc : gcBeans) {
            long count = gc.getCollectionCount();
            if (count != -1) {
                total += count;
            }
        }
        return total;
    }
    
    private static class TestResult {
        final double tps;
        final int gcCount;
        
        TestResult(double tps, int gcCount) {
            this.tps = tps;
            this.gcCount = gcCount;
        }
    }
    
    private static class GcTestResult {
        final double timeMs;
        final int gcCount;
        
        GcTestResult(double timeMs, int gcCount) {
            this.timeMs = timeMs;
            this.gcCount = gcCount;
        }
    }
}
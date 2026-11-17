# frogspawn JMH 基准报告：Heap / OffHeap × JDK8 / JDK25

- **日期**：2026-11-17
- **序号**：001
- **基准类**：`cn.itcraft.frogspawn.offheap.AllPoolsBenchmark`
- **JMH 版本**：1.32
- **模式**：Throughput（ops/us，越高越好）

## 1. 测试环境

| 项 | 值 |
|----|----|
| CPU | Intel Core Ultra 9 285H，16 vCPU |
| 内存 | 46 GiB |
| JDK8 | Oracle JDK 1.8.0_371 |
| JDK25 | Temurin OpenJDK 25.0.2+10 |
| 堆参数 | `-Xmx4G -Xms4G -Xmn2G` |
| 其他 JVM 参数 | `-Dfrogspawn.cache.capacity=64 -XX:-RestrictContended` |

JDK25 运行时确认加载：

```
.../META-INF/versions/24/cn/itcraft/frogspawn/platform/MemoryAccessImpl.class
```

即 JDK25 走 `MemorySegment` 覆盖层，未使用 `sun.misc.Unsafe`。

## 2. 测试参数

| 参数 | 值 |
|------|----|
| Warmup | 10 iterations × 100 ms |
| Measurement | 5 iterations × 20 ms |
| Threads | 4 |
| Fork | 3 |
| 结果单位 | ops/us |

## 3. 四个场景

| 编号 | 场景 | 对象 |
|------|------|------|
| test0 | Heap Pool（`LocalObjectsMemoryPoolImpl`） | `SimplePrimitiveStructure` |
| test1 | OffHeap Pool（`OffHeapObjectsMemoryPoolImpl`） | `SimplePrimitiveStructure` |
| test2 | FixedOffHeap Pool（`FixedOffHeapPool`） | `SimpleFixedObject` |
| test3 | `new`（基线） | `SimplePrimitiveStructure` |

## 4. 结果

| 场景 | JDK8 (ops/us) | JDK25 (ops/us) | JDK25 / JDK8 |
|------|--------------:|---------------:|-------------:|
| Heap Pool | 666.94 ± 18.87 | 698.53 ± 5.06 | **1.047x** |
| OffHeap Pool | 619.21 ± 6.44 | 678.98 ± 6.08 | **1.097x** |
| FixedOffHeap Pool | 300.25 ± 0.36 | 295.75 ± 3.74 | **0.985x** |
| new | 363.17 ± 19.82 | 359.22 ± 24.98 | 0.989x |

相对 `new` 的倍数：

| 场景 | JDK8 | JDK25 |
|------|-----:|------:|
| Heap Pool | 1.84x | 1.94x |
| OffHeap Pool | 1.70x | 1.89x |
| FixedOffHeap Pool | 0.83x | 0.82x |

## 5. 观察

1. **JDK25 的 Heap / OffHeap 均优于 JDK8**
   - Heap：+4.7%
   - OffHeap：+9.7%
   - OffHeap 与 Heap 的差距从 JDK8 的约 7.2% 缩小到 JDK25 的约 2.8%。
   - 说明 JDK25 的 `versions/24` MemorySegment 实现没有引入性能退化，反而受益于 JIT/运行时改进。

2. **FixedOffHeapPool 在 JDK25 略低（约 -1.5%）**
   - 差距接近测量误差范围，且 FixedOffHeap 使用不同的对象类型（`SimpleFixedObject`），不能与 Heap/OffHeap 的对象直接等价比较。
   - 当前 FixedOffHeap 使用 `AtomicIntegerArray` 做归属/重复释放校验，并用手工锁保护共享栈，这是为修复 P1 生命周期问题付出的正确性成本。
   - 与历史记录（约 277-299 ops/us）基本一致，未出现明显回退。

3. **Heap / OffHeap 均显著高于 `new`**
   - JDK25 下 Heap 约为 `new` 的 1.94x，OffHeap 约为 1.89x。
   - OffHeap 已接近 Heap，说明平台 SPI 与 MemorySegment 路径的抽象成本可控。

## 6. 原始数据

- JDK8：`/tmp/opencode/review/jmh-jdk8.json`
- JDK25：`/tmp/opencode/review/jmh-jdk25.json`
- 运行日志：`/tmp/opencode/review/jmh-jdk8.log`、`/tmp/opencode/review/jmh-jdk25.log`

## 7. 复现命令

```bash
cd frogspawn-test
export JAVA_HOME=/path/to/jdk25
mvnd clean test-compile dependency:build-classpath \
  -Dmdep.outputFile=/tmp/jmh-cp.txt

CP="target/test-classes:$(cat /tmp/jmh-cp.txt)"
java -cp "$CP" org.openjdk.jmh.Main AllPoolsBenchmark \
  -wi 10 -i 5 -w 100ms -r 20ms -t 4 -f 3 \
  -rf json -rff /tmp/jmh-result.json
```

> 注：基准使用 4 线程共享池，线程本地缓存按线程隔离；结果为吞吐量，不是单线程延迟。

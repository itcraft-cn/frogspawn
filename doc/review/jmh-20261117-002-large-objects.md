# frogspawn JMH 基准报告：超大对象（1MB）Heap / OffHeap / FixedOffHeap / new

- **日期**：2026-11-17
- **序号**：002
- **基准类**：`cn.itcraft.frogspawn.offheap.LargeObjectBenchmark`
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

JDK25 运行时使用 `META-INF/versions/24` 的 `MemorySegment` 实现。

## 2. 对象与参数

对象大小：**1MB**

| 场景 | 实现 |
|------|------|
| Heap Pool | `LargeHeapObject`：`long[131072]` 堆内 payload |
| OffHeap Pool | `LargeOffHeapObject`：1MB 原生内存作为对象主体 |
| FixedOffHeap Pool | `LargeFixedOffHeapObject`：每对象 1MB 固定原生块 |
| new | `new LargeHeapObject()` |

- 池容量：256
- Warmup：10 iterations × 100 ms
- Measurement：5 iterations × 20 ms
- Threads：4
- Fork：3
- `reset()` 不计入 1MB payload 清零；每次操作只访问 payload 首字。
  因此本基准衡量的是**池分配/GC 与地址管理**，不是内存带宽。

## 3. 结果

| 场景 | JDK8 (ops/us) | JDK25 (ops/us) | JDK25 / JDK8 |
|------|--------------:|---------------:|-------------:|
| Heap Pool | 645.555 ± 30.998 | 610.299 ± 67.057 | 0.945x |
| OffHeap Pool | 600.378 ± 21.806 | **673.156 ± 53.142** | **1.121x** |
| FixedOffHeap Pool | 263.150 ± 26.971 | 291.873 ± 4.371 | 1.109x |
| new | 0.035 ± 0.007 | 0.031 ± 0.005 | 0.880x |

相对 `new` 的倍数（数量级参考）：

| 场景 | JDK8 | JDK25 |
|------|-----:|------:|
| Heap Pool | ≈ 18,400x | ≈ 19,700x |
| OffHeap Pool | ≈ 17,200x | ≈ 21,700x |
| FixedOffHeap Pool | ≈ 7,500x | ≈ 9,400x |

> 该倍数主要反映“复用 1MB 内存”与“每次 `new` 分配/清零 1MB + GC”的差异，
> 不是纯 `fetch/release` 开销的倍数。

## 4. GC 观察（1 fork × 3 iterations，方向性）

| 场景 | JDK8 GC | JDK25 GC |
|------|---------|----------|
| Heap / OffHeap / FixedOffHeap | ≈ 0 | ≈ 0 |
| new | 1 次，约 3 ms | 6 次，约 21 ms |

- `new` 的分配速率约 1688 MB/s（JDK8）/ 571 MB/s（JDK25）。
- 池化场景在测量窗口内基本无 GC。
- 该组数据为 1 fork、3 iterations，噪声较大，仅用于确认方向。

## 5. 结论

1. **超大对象场景是池化的主场**
   1MB 对象的 `new` 吞吐只有约 0.03 ops/us（约 3 万 ops/s），而池化 fetch/release
   达到数百 ops/us。差距来自避免每次 1MB 分配/清零与 GC。

2. **JDK25 的 MemorySegment OffHeap 表现最好**
   - OffHeap: 600.378 → 673.156 ops/us（+12.1%）
   - FixedOffHeap: 263.150 → 291.873 ops/us（+10.9%）
   - Heap 的 JDK25 数据误差较大（±67），与 JDK8 的差异不具结论性。

3. **FixedOffHeap 吞吐低于 Heap/OffHeap**
   当前 FixedOffHeap 有归属校验（`AtomicIntegerArray` CAS）和共享栈锁，这是正确性成本；
   它仍比 `new` 快约 3 个数量级。其价值在固定地址、预创建零分配、大对象与 GC 敏感场景。

4. **Heap 仍是小对象默认首选**
   本报告只说明 1MB 大对象下池化相对 `new` 的巨大优势；小对象场景仍应优先 Heap。

## 6. 原始数据

- JDK8：`/tmp/opencode/review/jmh-large-jdk8.json`
- JDK25：`/tmp/opencode/review/jmh-large-jdk25.json`
- GC profile：`/tmp/opencode/review/jmh-large-gc-jdk8.json`、`jmh-large-gc-jdk25.json`
- 日志：`/tmp/opencode/review/jmh-large-*.log`

## 7. 复现命令

```bash
cd frogspawn-test
export JAVA_HOME=/path/to/jdk25
mvnd clean test-compile dependency:build-classpath \
  -Dmdep.outputFile=/tmp/jmh-cp.txt

CP="target/test-classes:$(cat /tmp/jmh-cp.txt)"
java -cp "$CP" org.openjdk.jmh.Main LargeObjectBenchmark \
  -wi 10 -i 5 -w 100ms -r 20ms -t 4 -f 3 \
  -rf json -rff /tmp/jmh-large.json
```

# `frogspawn` 蛙卵

[English version](README_en.md) | [使用手册](MANUAL.md) | [更新记录](CHANGELOG.md)

**期望**：

> 如青蛙产卵一般，密集产出大量卵，供后续使用

**目标**：

> 作为内存对象池，预分配预处理

**使用场景**

> 低延迟场合下，用以代替分配大量无状态小对象。防止GC导致的延迟波动。

比如：接口序列化/反序列化时

[SerializeCompareSample](frogspawn-test/src/test/java/cn/itcraft/frogspawn/sample/serial/SerializeCompareSample.java)

## 使用方法

受管理对象需实现一个特定接口 `Resettable`

```java
public class DemoPojo implements Resettable {
    ...
}
```

需要实现一个创建器

```java
public class DemoPojoCreator implements ObjectCreator<DemoPojo> {
    @Override
    public DemoPojo create() {
        return new DemoPojo();
    }
}
```

构建对象池

```java
PoolStrategy strategy = new PoolStrategy(
        FetchStrategy.MUST_FETCH_IN_POOL, FetchFailStrategy.CALL_CREATOR);
ObjectsMemoryPool<DemoPojo> pool =
        ObjectsMemoryPoolFactory.newPool(new DemoPojoCreator(), SIZE, strategy);
```

使用

```java
try{
    DemoPojo pojo = pojoPool.fetch();
    // TODO using pojo
    ...
}finally{
    pojoPool.release(pojo);
}
```

## 统一池创建 API

`fetch` / `release` 对所有池类型已经一致。统一创建入口：

```java
ObjectsMemoryPool<DemoPojo> pool = ObjectsMemoryPoolFactory.newPool(
        new DemoPojoCreator(), PoolConfig.offHeap(1024));
```

只改配置即可切换实现：

```java
PoolConfig.heap(3000);
PoolConfig.offHeap(1024);
PoolConfig.fixedOffHeap(10000, 64);
```

`OFF_HEAP` 要求对象实现 `PrimitiveStructure`；`FIXED_OFF_HEAP` 要求实现 `FixedOffHeapObject`。工厂会校验 creator 的返回类型，不匹配时抛出 `IllegalArgumentException`。原有 `newPool` / `newOffHeapPool` / `newFixedOffHeapPool` 方法继续保留，兼容旧代码。

## 三种可选模式

目前支持三种不同模式：

1. 必定从池中取

    ```java
    PoolStrategy strategy = new PoolStrategy(
        FetchStrategy.MUST_FETCH_IN_POOL, FetchFailStrategy.CALL_CREATOR);
    ObjectsMemoryPool<DemoPojo> pool =
        ObjectsMemoryPoolFactory.newPool(new DemoPojoCreator(), SIZE, strategy);
    ```

1. 取一定次数后返空

    ```java
    PoolStrategy strategy = new PoolStrategy(
        FetchStrategy.FETCH_FAIL_AS_NULL, FetchFailStrategy.CALL_CREATOR);
    ObjectsMemoryPool<DemoPojo> pojoPool =
        ObjectsMemoryPoolFactory.newPool(new DemoPojoCreator(), SIZE, strategy);
    ```

1. 取一定次数后创建新对象

    ```java
    PoolStrategy strategy = new PoolStrategy(
        FetchStrategy.FETCH_FAIL_AS_NEW, FetchFailStrategy.CALL_CREATOR);
    ObjectsMemoryPool<DemoPojo> pojoPool =
        ObjectsMemoryPoolFactory.newPool(new DemoPojoCreator(), SIZE, strategy);
    ```

默认方案为最后一种。

## 可选参数

- `-Dfrogspawn.fetch.times`，最大循环取次数，默认值：10
- `-Dfrogspawn.max.capacity`，池最大容量，默认值：1048576
- `-Dfrogspawn.cache.capacity`，线程缓存容量，默认值：8，最大值：64。设置为 `1` 能获得最大性能。

## Java 版本与 MR-JAR

`frogspawn` 以 **Multi-Release JAR** 形式发布，base 层为 JDK8：

| 层 | 源码目录 | Class 版本 | 生效 JDK |
|----|---------|-----------|----------|
| 公共 + JDK8 | `src/main/java` + `src/main/java8` | 52 (Java 8) | JDK8+ |
| JDK9+ 覆盖层 | `src/main/java9` | 53 (Java 9) | JDK9+ |
| JDK24+ 覆盖层 | `src/main/java24` | 68 (Java 24) | JDK24+ |

- `src/main/java`：只放与 JDK 版本无关的代码。
- `src/main/java8`：JDK8 专有实现，包括 `sun.misc.Unsafe`、`sun.misc.Contended`、`RawMemoryUtil`。
- `src/main/java9`：JDK9+ 覆盖实现，包括 `jdk.internal.vm.annotation.Contended` 以及基于 `VarHandle` 的 `MemoryAccessImpl`。
- `src/main/java24`：JDK24+ 覆盖实现，使用 `java.lang.foreign.MemorySegment`，完全不使用 `sun.misc.*`。

平台相关访问统一走 `cn.itcraft.frogspawn.platform.MemoryAccess`：
- JDK8 使用 `sun.misc.Unsafe`。
- JDK9-23 使用 `VarHandle` 访问数组，原生内存使用 `RawMemoryUtil`。
- JDK24+ 使用 `MemorySegment` 完成分配/释放、基本类型访问与 `copyFrom`，数组访问使用 `VarHandle`，JDK24+ 实现不再加载 `sun.misc.Unsafe`。
- `FixedOffHeapObject.buffer()` 提供可选的零拷贝 `ByteBuffer` 视图：JDK24+ 通过 `MemorySegment.asByteBuffer()` 创建，JDK8-23 可由实现持有 direct `ByteBuffer`。

同时使用 JDK8 与 JDK24+ 构建完整 MR-JAR：

```bash
export JAVA_HOME8=/path/to/jdk8
export JAVA_HOME=/path/to/jdk24+
mvn -f frogspawn/pom.xml clean package
```

可用 `-Dmr.base.jdk.home=/path/to/jdk8` 覆盖 `JAVA_HOME8`。只用 JDK8 构建也能得到合法的 MR-JAR，只是不含 `META-INF/versions/9` 与 `META-INF/versions/24` 覆盖层。

## 伪共享相关

- JDK8 下 base 类使用 `sun.misc.Contended`。
- JDK9+ 下覆盖类使用 `jdk.internal.vm.annotation.Contended`。
- 另有文档说，`@Contended` 注解需要 `JVM` 为用户类开启参数 `-XX:-RestrictContended`。

## 性能测试结果

### JMH 吞吐对比（4 场景）

环境：Intel Core Ultra 9 285H / 16 vCPU，4GB heap，4 threads，10×100ms warmup，5×20ms measurement，fork 3。JDK8 使用 base 实现，JDK25 使用 `versions/24` 的 `MemorySegment` 实现。

| 场景 | JDK8 (ops/us) | JDK25 (ops/us) | JDK25 / JDK8 |
|------|--------------:|---------------:|-------------:|
| Heap Pool | 666.94 | 698.53 | 1.047x |
| OffHeap Pool | 619.21 | 678.98 | 1.097x |
| FixedOffHeap Pool | 300.25 | 295.75 | 0.985x |
| new（基线） | 363.17 | 359.22 | 0.989x |

结论：

- 小对象、无 GC 压力时 **Heap 最快**；JDK25 下 OffHeap 已接近 Heap（约 97%）。
- FixedOffHeap 在微基准中吞吐较低，其价值在固定地址、预创建零分配、大对象与 GC 敏感场景。
- OffHeap/FixedOffHeap 的主场是 GC 压力与堆外内存，而不是 8 字节对象的纯 fetch/release 吞吐。
- 超大对象（1MB）结果见 [JMH 大对象报告](doc/review/jmh-20261117-002-large-objects.md)。

### GC 压力测试（历史数据）

环境：OpenJDK 25，4GB heap，warmup 100k iterations，test 1M iterations。

小对象（~64 bytes）：

| 方案 | 时间 (ms) | GC 次数 |
|------|----------:|--------:|
| **FixedOffHeapPool** | **17.59** | **0** |
| new | 27.37 | 0 |
| HeapPool | 118.65 | 0 |

大对象（~2KB）：

| 方案 | 时间 (ms) | GC 次数 |
|------|----------:|--------:|
| **FixedOffHeapPool** | **82.32** | **0** |
| new | 127.91 | 1 |
| HeapPool | 645.25 | 0 |

GC 压力结论：

- **FixedOffHeapPool** 在大对象与 GC 敏感场景最优，零 GC，原生 reset 走 `MemoryAccess.setMemory`（JDK8-23 为 `UNSAFE.setMemory`，JDK24+ 为 `MemorySegment.fill`）。
- **HeapPool** 的大对象 reset（数组清零）开销可能高于 JVM 新建对象。
- **new** 在压力下可能触发 GC。

### Heap 模式的最大优势：平滑 GC 抖动

Heap 模式最大的优势不是纯 `fetch/release` 吞吐，而是**平滑 GC 带来的延迟抖动**：

- 预分配 + 复用降低分配速率；
- 线程本地缓存降低共享池竞争；
- 减少 GC 次数与 pause，使 p95 / p99.9 更稳定。

在固定线程池、固定处理链路的低延迟系统中，这种“方差优化”通常比“均值优化”更有价值。

具体生产数据因环境、负载与 GC 配置而异；frogspawn 的贡献应通过 GC 次数/停顿、分配速率、以及 p99.9 与 GC pause 的相关性来验证。

## 选型认知

- **Heap 是默认选择**：小对象用于平滑 GC 抖动；大对象可用于复用、降低分配/清零成本。
- **小对象**：池化的收益主要是降低分配速率、GC 频率与尾延迟，不是提升平均吞吐；微基准中 `new` 往往更快。
- **大对象**：Heap 池化不是无条件更优。对象常驻堆会提高存活集/老年代压力；如果 `reset()` 需要清零大块内存，收益会明显缩小。GC 敏感的大对象优先考虑 OffHeap / FixedOffHeap。
- **对比基线**：首先是 `new` 与 GC 指标（次数/停顿/p99.9），其次才是通用池（如 Commons Pool 2.x）。
- **OffHeap / FixedOffHeap 是特化**：
  - Dynamic OffHeap：变长/大块数据、GC offload、native 互操作；
  - FixedOffHeap：定长记录、固定地址、零分配、零拷贝磁盘/网络 IO。
  - 适用面窄，但在匹配场景价值最高。

## 相关文档

- [使用手册](MANUAL.md)
- [更新记录](CHANGELOG.md)

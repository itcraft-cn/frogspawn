# frogspawn 使用手册

[English version](MANUAL_en.md) | [README](README.md) | [更新记录](CHANGELOG.md)

本手册覆盖 frogspawn 的对象契约、统一创建 API、三种池的选型、MR-JAR 构建与常见问题。

## 1. 环境要求

| 用途 | 要求 |
|------|------|
| 运行 | JDK8 及以上 |
| 完整 MR-JAR 构建 | JDK8（编译 base）+ JDK24+（编译 `versions/9` / `versions/24`） |
| 构建工具 | 首选 `mvnd`，次选 `mvn` |

JDK24+ 运行时会自动加载 `META-INF/versions/24`，原生内存由 `java.lang.foreign.MemorySegment` 管理，不再加载 `sun.misc.Unsafe`。

## 2. 依赖与构建

Maven 坐标（当前开发版本）：

```xml
<dependency>
    <groupId>cn.itcraft</groupId>
    <artifactId>frogspawn</artifactId>
    <version>0.7-dev</version>
</dependency>
```

构建完整 MR-JAR：

```bash
export JAVA_HOME8=/path/to/jdk8
export JAVA_HOME=/path/to/jdk24+
mvn -f frogspawn/pom.xml clean install -DskipTests
```

指定 JDK8 路径：

```bash
mvn -f frogspawn/pom.xml clean package -Dmr.base.jdk.home=/path/to/jdk8
```

运行测试：

```bash
cd frogspawn-test
mvn test
```

## 3. 核心概念

| 类型 | 作用 |
|------|------|
| `Resettable` | 所有池对象的基础契约：`reset()`、`isInvalid()`、`markId/getMarkedId` |
| `ObjectCreator<T>` | 创建对象实例 |
| `ObjectsMemoryPool<T>` | 统一使用接口：`fetch()` / `release(T)` |
| `PoolType` | `HEAP` / `OFF_HEAP` / `FIXED_OFF_HEAP` |
| `PoolConfig` | 池类型、容量、对象大小与堆池策略 |
| `PrimitiveStructure` | 动态非堆对象契约 |
| `FixedOffHeapObject` | 固定非堆对象契约 |

## 4. 快速开始：Heap 池

```java
public class DemoPojo implements Resettable {
    private int value;

    @Override
    public void reset() {
        value = 0;
    }

    @Override
    public int getMarkedId() {
        return -1;
    }

    @Override
    public void markId(int id) {
    }
}

ObjectsMemoryPool<DemoPojo> pool = ObjectsMemoryPoolFactory.newPool(
        new ObjectCreator<DemoPojo>() {
            @Override
            public DemoPojo create() {
                return new DemoPojo();
            }
        },
        PoolConfig.heap(3000));

DemoPojo pojo = pool.fetch();
try {
    // use pojo
} finally {
    pool.release(pojo);
}
```

## 5. 统一创建 API

`fetch` / `release` 对三种池完全一致；创建时只改 `PoolConfig`。

### 5.1 Heap

```java
ObjectsMemoryPool<DemoPojo> pool = ObjectsMemoryPoolFactory.newPool(
        new DemoPojoCreator(), PoolConfig.heap(3000));
```

指定堆池策略：

```java
PoolStrategy strategy = new PoolStrategy(
        FetchStrategy.FETCH_FAIL_AS_NEW, FetchFailStrategy.CALL_CREATOR);
ObjectsMemoryPool<DemoPojo> pool = ObjectsMemoryPoolFactory.newPool(
        new DemoPojoCreator(), PoolConfig.heap(3000, strategy));
```

### 5.2 Dynamic OffHeap

```java
ObjectsMemoryPool<MyOffHeapPojo> pool = ObjectsMemoryPoolFactory.newPool(
        MyOffHeapPojo::new, PoolConfig.offHeap(1024));
```

要求 `MyOffHeapPojo` 实现 `PrimitiveStructure`。

### 5.3 Fixed OffHeap

```java
ObjectsMemoryPool<MyFixedPojo> pool = ObjectsMemoryPoolFactory.newPool(
        MyFixedPojo::new, PoolConfig.fixedOffHeap(10000, 64));
```

要求 `MyFixedPojo` 实现 `FixedOffHeapObject`；`64` 为每个对象占用的字节数。

### 5.4 兼容旧 API

以下方法保留并内部委托到统一入口：

```java
newPool(creator, size)
newPool(creator, size, strategy)
newOffHeapPool(creator)
newOffHeapPool(creator, maxPoolSize)
newFixedOffHeapPool(creator, capacity, objectSize)
```

## 6. 对象契约

### 6.1 Resettable

- `reset()` 在 `release` 时自动调用。
- `reset()` 不得清除池管理的身份字段：
  - Heap 池的 `markId`（由池设置，用于归还定位）；
  - FixedOffHeap 的 `index` / `address`（由池设置，清空会导致对象失效）。
- `isInvalid()` 返回 `true` 时，Heap 池会丢弃缓存对象并从主池重新获取。

### 6.2 PrimitiveStructure（动态非堆）

必须实现：

- `getAddress()` / `setAddress(long)`：池分配地址后回填。
- `getObjectSize()`：对象主体字节数。
- `getArrayFieldCount()` / `getArrayLength(int)` / `getArrayAddress(int)` /`setArrayAddress(int, long)`：数组字段管理。
- `getArrayElementSize(int)`：数组元素宽度，默认 8 字节；`int[]` 返回 4，`byte[]` 返回 1。
- `writeToMemory()` / `readFromMemory()`：对象与原生内存之间的读写。
- `hasDataToWrite()`：可返回 `false` 跳过写入。

数组内存由池按 `length × elementSize` 分配，实现不要再自行分配。

### 6.3 FixedOffHeapObject（固定非堆）

必须实现：

- `index()` / `index(int)`：池分配的固定索引。
- `address()` / `address(long)`：池分配的固定地址。
- `reset()`：只重置业务字段，**不要**清空 `index` / `address`。

`FixedOffHeapPool` 预创建全部对象，并持有整块原生内存；对象地址在池生命周期内固定。池实现 `AutoCloseable`，不再使用时调用 `close()`：

```java
ObjectsMemoryPool<MyFixedPojo> pool = ObjectsMemoryPoolFactory.newPool(
        MyFixedPojo::new, PoolConfig.fixedOffHeap(10000, 64));
try {
    // fetch / release
} finally {
    ((AutoCloseable) pool).close();
}
```

`close()` 后再 `fetch/release` 会抛出 `IllegalStateException`。

#### 零拷贝 ByteBuffer 视图

`FixedOffHeapObject.buffer()` 默认抛 `UnsupportedOperationException`，实现类可按需覆盖：

- **JDK24+**：使用 `PlatformMemory.asByteBuffer(address(), OBJECT_SIZE)`，底层是 `MemorySegment.asByteBuffer()`，可直接用于 `FileChannel` / `SocketChannel`：

  ```java
  @Override
  public ByteBuffer buffer() {
      return PlatformMemory.asByteBuffer(address(), OBJECT_SIZE);
  }
  ```

- **JDK8-23**：没有公开 API 将任意地址包装为 `ByteBuffer`；建议实现类持有 `ByteBuffer.allocateDirect(OBJECT_SIZE)`，`buffer()` 直接返回，并把 `address()` 与该 buffer 关联。

注意：

- 每个 fetch 到的对象/ByteBuffer 必须独占使用，不要跨线程共享；
- `position/limit` 是 ByteBuffer 自身状态，每次 IO 前按需 `clear()` / `flip()`；
- `FixedOffHeapPool.close()` 后不要再使用任何 buffer 视图。

## 7. 池策略

`FetchStrategy`：

| 策略 | 行为 |
|------|------|
| `MUST_FETCH_IN_POOL` | 阻塞循环，直到从池中取到对象 |
| `FETCH_FAIL_AS_NULL` | 重试 `frogspawn.fetch.times` 次后返回 `null` |
| `FETCH_FAIL_AS_NEW` | 重试 `frogspawn.fetch.times` 次后调用 creator 新建（默认） |

`FetchFailStrategy`：`NOT_AVAILABLE` / `NULLABLE` / `CALL_CREATOR`。

策略目前只作用于 Heap 池；OffHeap 与 FixedOffHeap 在容量不足时按各自语义新建或抛出 `IllegalStateException`。

## 8. 系统属性

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `-Dfrogspawn.fetch.times` | `10` | 最大获取尝试次数 |
| `-Dfrogspawn.max.capacity` | `1048576` | Heap 池最大容量 |
| `-Dfrogspawn.cache.capacity` | `8` | 线程本地缓存容量，最大 `64`；设为 `1` 性能最高 |

伪共享注解 `@Contended` 对用户类生效通常还需要：

```bash
-XX:-RestrictContended
```

## 9. MR-JAR 构建与运行

源码分层：

| 目录 | 内容 | Class 版本 | 运行时 |
|------|------|-----------|--------|
| `src/main/java` | 平台无关代码 | 52 | JDK8+ |
| `src/main/java8` | Unsafe / Contended / RawMemoryUtil | 52 | JDK8+ |
| `src/main/java9` | VarHandle / `jdk.internal` Contended | 53 | JDK9+ |
| `src/main/java24` | MemorySegment / VarHandle | 68 | JDK24+ |

运行时由 JVM 自动选择最高可用版本：

- JDK8：base（Unsafe）
- JDK9-23：`META-INF/versions/9`（VarHandle + RawMemoryUtil）
- JDK24+：`META-INF/versions/24`（MemorySegment，无 Unsafe）

构建产物的 `sources.jar` 同时包含 `java8`、`versions/9`、`versions/24` 源码。

## 10. 性能与选型

JMH（4 threads、10×100ms warmup、5×20ms measurement、fork 3）：

| 场景 | JDK8 (ops/us) | JDK25 (ops/us) | JDK25 / JDK8 |
|------|--------------:|---------------:|-------------:|
| Heap Pool | 666.94 | 698.53 | 1.047x |
| OffHeap Pool | 619.21 | 678.98 | 1.097x |
| FixedOffHeap Pool | 300.25 | 295.75 | 0.985x |
| new | 363.17 | 359.22 | 0.989x |

选型建议：

- 小对象、短生命周期、吞吐优先：**Heap**。
- 大对象、大容量、GC 敏感、尾延迟敏感：**OffHeap**。
- 固定大小记录、需要稳定地址、零分配、native/网络/序列化互操作：**FixedOffHeap**。
- FixedOffHeap 的微基准吞吐低于 Heap/OffHeap，这是固定地址与正确性校验的成本；它的价值在 GC 压力、固定地址与零分配场景。

### Heap 模式的最大优势：平滑 GC 抖动

Heap 模式最大的优势不是纯 `fetch/release` 吞吐，而是**平滑 GC 带来的延迟抖动**：

- 预分配 + 复用降低分配速率；
- 线程本地缓存降低共享池竞争；
- 减少 GC 次数与 pause，使 p95 / p99.9 更稳定。

在固定线程池、固定处理链路的低延迟系统中，这种“方差优化”通常比“均值优化”更有价值。

具体生产数据因环境、负载与 GC 配置而异；frogspawn 的贡献应通过 GC 次数/停顿、分配速率、以及 p99.9 与 GC pause 的相关性来验证。

更多数据见 [JMH 报告](doc/review/jmh-20261117-001-heap-offheap.md)。

## 11. 常见问题

### 11.1 FixedOffHeapPool exhausted

固定池容量耗尽时抛 `IllegalStateException`。请提高 `PoolConfig.fixedOffHeap(capacity, ...)` 的容量，或确保每个 `fetch()` 都有对应的 `release()`。

### 11.2 Object does not belong to this pool

`release` 只接受本池创建的对象，且不能重复释放。检查是否把对象还给了错误的池，或对同一对象调用了两次 `release`。

### 11.3 对象取到后是脏数据

- Heap 池：确认 `reset()` 已正确实现；`release` 会调用 `reset()`。
- FixedOffHeap：确认 `reset()` 只清业务字段，不清 `index` / `address`。
- OffHeap：确认 `readFromMemory()` 在读取业务数据前被调用。

### 11.4 JDK24+ 仍有 Unsafe 告警

JDK24+ 应加载 `META-INF/versions/24`。检查：

```bash
java -verbose:class -cp frogspawn.jar YourMain 2>&1 | grep MemoryAccessImpl
```

应看到 `MemoryAccessImpl$Allocation`（MemorySegment 实现）。如果加载的是 base 实现，确认 jar 的 manifest 含 `Multi-Release: true`。

## 12. 相关文档

- [README](README.md)
- [更新记录](CHANGELOG.md)
- [JMH 基准报告](doc/review/jmh-20261117-001-heap-offheap.md)
- [代码审查报告](doc/review/code-review-20261117-001-java.md)

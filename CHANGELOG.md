# 更新记录 CHANGELOG

[English version](CHANGELOG_en.md)

## v0.7.0 (2026-11-18)

### 构建
- 改造为基于 JDK8 的 **Multi-Release JAR**：
  - base 层由 JDK8 编译（`src/main/java` + `src/main/java8`）
  - JDK9+ 覆盖层输出到 `META-INF/versions/9`
  - jar 清单新增 `Multi-Release: true` 与 `Automatic-Module-Name`
- 源码按 JDK 依赖分层：
  - `src/main/java`：与 JDK 版本无关的代码
  - `src/main/java8`：`sun.misc.Unsafe`、`sun.misc.Contended` 等 JDK8 专有实现
  - `src/main/java9`：`jdk.internal.vm.annotation.Contended` 等 JDK9+ 覆盖实现
- 修复 JDK9+ 无法编译的问题（`sun.misc.Contended` 在 JDK9+ 已迁移）
- 升级 `central-publishing-maven-plugin` `0.7.0` → `0.12.0`，兼容 Maven Central API 的 `warnings` 字段
- `frogspawn-test` 依赖版本对齐到 `0.7.0`

### 修复与清理
- 修复堆池线程本地缓存路径未调用 `reset()` 的释放语义缺陷
- 删除与 `ClassChecker` 重复且无引用的 `ClassUtil`
- 修复未使用字段/局部变量、整型乘法溢出风险与裸异常类型
- 新增 `.astgrepignore` 登记有意 CAS/阻塞循环及测试输出的误报
- 新增 `LocalObjectsMemoryPoolResetTest` 回归测试

### 非堆内存与平台 SPI
- 新增 `cn.itcraft.frogspawn.platform.MemoryAccess` 平台 SPI：
  - `java8` 实现使用 `sun.misc.Unsafe`
  - `java9` 实现使用 `VarHandle` 访问数组，原生内存复用 `RawMemoryUtil`
  - `java24` 实现完全不使用 `sun.misc.*`，使用 `java.lang.foreign.MemorySegment` 分配/释放内存、基本类型访问与 `copyFrom`，数组访问使用 `VarHandle`
  - `ArrayUtil`、`FixedOffHeapPool`、`OffHeapObjectsMemoryPoolImpl` 不再直接依赖 `Unsafe`
- MR-JAR 新增 `META-INF/versions/24` 覆盖层
- `FixedOffHeapObject` 新增 `buffer()` 默认方法（默认抛 `UnsupportedOperationException`）；`PlatformMemory.asByteBuffer(address, size)` 在 JDK24+ 基于 `MemorySegment.asByteBuffer()` 提供零拷贝 `ByteBuffer` 视图

### API
- 新增统一池创建入口：
  - `PoolType`：`HEAP` / `OFF_HEAP` / `FIXED_OFF_HEAP`
  - `PoolConfig`：`heap(...)` / `offHeap(...)` / `fixedOffHeap(...)`
  - `ObjectsMemoryPoolFactory.newPool(creator, PoolConfig)`
- 原有工厂方法保留，内部委托到统一入口
- `fetch` / `release` 使用 API 保持统一；非堆类型在创建时校验对象接口
- `FixedOffHeapPool` 修复池耗尽/重试失败返回未托管对象、释放兜底覆盖索引的问题：
  - 耗尽抛 `IllegalStateException`
  - 增加归属校验与 `IN_USE/AVAILABLE` 状态 CAS
  - 共享栈改为锁保护，消除并发拷贝竞态
- `PrimitiveStructure` 新增默认 `getArrayElementSize`，数组按实际元素宽度分配
- `OffHeapMemInfo` 保留手工填充字节数并累计到 `totalPadding`
- `ClassChecker` 的 `Field.setAccessible` 移入 `AccessController.doPrivileged`
- `sources.jar` 增加 `META-INF/versions/9` 与 `META-INF/versions/24` 源码映射
- 新增 `FixedOffHeapPoolTest`、`UnifiedPoolApiTest` 回归测试

### 性能
- 新增统一 JMH 基准 `AllPoolsBenchmark`，覆盖 4 个场景：
  - Heap Pool / OffHeap Pool / FixedOffHeap Pool / `new`
- JDK8 与 JDK25 对比（4 threads、10×100ms warmup、5×20ms measurement、fork 3）：

| 场景 | JDK8 (ops/us) | JDK25 (ops/us) | JDK25 / JDK8 |
|------|--------------:|---------------:|-------------:|
| Heap Pool | 666.94 | 698.53 | 1.047x |
| OffHeap Pool | 619.21 | 678.98 | 1.097x |
| FixedOffHeap Pool | 300.25 | 295.75 | 0.985x |
| new | 363.17 | 359.22 | 0.989x |

- JDK25 的 `versions/24` MemorySegment 实现无性能回退；OffHeap 已接近 Heap。
- FixedOffHeap 的微基准吞吐较低，价值在固定地址、零分配、大对象与 GC 敏感场景。
- 新增 1MB 超大对象 JMH 基准：池化 fetch/release 比 `new` 快约 3 个数量级；JDK25 OffHeap（MemorySegment）达到 673 ops/us。详见 `doc/review/jmh-20261117-002-large-objects.md`。

### 文档
- README 拆分为中文 `README.md` / 英文 `README_en.md`
- 更新记录拆分为中文 `CHANGELOG.md` / 英文 `CHANGELOG_en.md`
- 新增中文 `MANUAL.md` / 英文 `MANUAL_en.md` 使用手册
- 明确 Heap 模式最大优势是平滑 GC 带来的延迟抖动；具体生产数据因环境、负载与 GC 配置而异，公开文档不记录具体数字

## v0.6

### 性能优化
- **SimpleStackCache 替代 SoftRefStore**: 新的栈式缓存实现，O(1) 时间复杂度的 fetch/release 操作，性能显著提升 (~479% throughput 提升)
- **移除 prefetch 功能**: 简化池实现，减少不必要的复杂性，进一步优化性能

### 重构
- 简化 `ObjectsMemoryPoolImpl` 实现，代码更清晰
- 优化 `PoolStrategy` 接口，移除 prefetch 参数

### 测试
- 添加多线程基准测试 `MultiObjectsMemoryPoolBenchmark`
- 添加性能测试脚本 `run_bench.sh`，支持 JMH benchmark

### 文档
- 添加英文版本文档 (`README_en.md`)
- 添加 Java 代码走查报告 (`doc/review/java-review-20260326-001.md`)
- 代码走查报告涵盖设计模式、线程安全、性能分析、代码规范等方面

### 构建准备
- 版本号更新至 0.6

---

## v0.5

- refactor, just one pool impl.

## v0.4

- first deploy!

## v0.3

- 支持 isInvalid 检查

## v0.2

- 添加自动填充模式：一个线程从池里消费，一个线程返还到池里的场景，可以使用此模式。

## v0.1

- 首次发布


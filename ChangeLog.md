# 更新记录ChangeLog

## v0.7 (开发中)

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
- `frogspawn-test` 依赖版本对齐到 `0.7-dev`

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
  - `ArrayUtil`、`FixedOffHeapPool`、`OffHeapObjectsMemoryPoolImpl` 不再直接依赖 `Unsafe`
- `FixedOffHeapPool` 修复池耗尽/重试失败返回未托管对象、释放兜底覆盖索引的问题：
  - 耗尽抛 `IllegalStateException`
  - 增加归属校验与 `IN_USE/AVAILABLE` 状态 CAS
  - 共享栈改为锁保护，消除并发拷贝竞态
- `PrimitiveStructure` 新增默认 `getArrayElementSize`，数组按实际元素宽度分配
- `OffHeapMemInfo` 保留手工填充字节数并累计到 `totalPadding`
- `ClassChecker` 的 `Field.setAccessible` 移入 `AccessController.doPrivileged`
- `sources.jar` 增加 `META-INF/versions/9` 源码映射
- 新增 `FixedOffHeapPoolTest` 回归测试

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
- 添加英文版本文档 (README.md)
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


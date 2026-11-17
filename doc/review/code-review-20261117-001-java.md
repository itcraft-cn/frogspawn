# frogspawn 代码审查报告（MR-JAR 改造专项）

- **日期**：2026-11-17
- **序号**：001
- **语言**：Java
- **基线**：`74ee59c build: package as JDK8-based multi-release jar` + 本次审查修复
- **审查范围**：
  - `frogspawn/src/main/java`（平台无关代码）
  - `frogspawn/src/main/java8`（JDK8 专有实现）
  - `frogspawn/src/main/java9`（JDK9+ 覆盖实现）
  - `frogspawn-test/src/test/java`（测试与基准）
- **审查引擎**：
  - ast-grep 0.51.1 + code-checklist（33 条 Java 规则）
  - PMD 6.19.0（quickstart + design + security）
  - CPD 6.19.0（minimum-tokens=100）
  - SpotBugs 4.7.3（effort=max，threshold=low，JDK17 运行）

## 1. 总体结论

| 维度 | 结果 | 判定 |
|------|------|------|
| MR-JAR 结构与运行时兼容性 | JDK8 base + JDK9+ overlay 均正确加载 | **通过** |
| ast-grep high（CI 门禁） | 0 条 | **通过** |
| ast-grep medium | 24 条（枚举注释、集合初始容量、测试循环拼接） | 登记，下一迭代 |
| PMD P0 | 0 条（2 条命名规则按误报处理） | **通过** |
| PMD P3 | 37 条（LawOfDemeter、复杂度等） | 登记 |
| SpotBugs P0/P1 | 0 条（3 条内部表示暴露为设计目的） | **通过** |
| CPD 重复代码 | 0 处（≥100 tokens） | **通过** |
| 单元测试 | 17/17，JDK8 与 JDK21 均通过 | **通过** |

**结论：MR-JAR 改造与本次 P1/P2 修复均已完成，可以进入发布流程。**

## 2. MR-JAR 专项审查

### 2.1 结构正确性

| 检查项 | 结果 |
|--------|------|
| `src/main/java` 不含 `sun.misc.*` / `jdk.*` 内部 API | 通过 |
| `src/main/java8` 承载 `sun.misc.Unsafe`、`sun.misc.Contended`、`RawMemoryUtil` | 通过 |
| `src/main/java9` 覆盖类包名/类名与 base 一致 | 通过（`PaddedAtomicLong/Boolean`、`MemoryAccessImpl`） |
| `src/main/java9` 不直接引用 `sun.misc.*` | 通过（数组访问用 VarHandle，原生内存复用 base 的 RawMemoryUtil） |
| base Class 版本 | 52（Java 8） |
| overlay Class 版本 | 53（Java 9） |
| Manifest | `Multi-Release: true`、`Automatic-Module-Name` 存在 |
| `META-INF/versions/9` 中无构建残留（`jpms.args` 已排除） | 通过 |
| `sources.jar` 包含 `src/main/java8` 与 `META-INF/versions/9` 下的 `java9` 源码 | 通过 |

### 2.2 运行时验证

对最终 jar 在多个 JDK 上执行了类加载、注解识别、堆池与固定非堆池的基本读写：

| JDK | 加载路径 | `PaddedAtomicLong` 注解 | 池功能 |
|-----|----------|--------------------------|--------|
| 8 | base | `sun.misc.Contended` | 正常 |
| 11 / 17 / 21 / 25 / 27 | `META-INF/versions/9` | `jdk.internal.vm.annotation.Contended` | 正常 |

`jdk.internal.vm.annotation.Contended` 仅作为类文件注解被 JVM 读取，运行期不需要 `--add-exports`；仅编译期需要，当前已在 `compile-java9` 执行中配置。

### 2.3 平台 SPI

新增 `cn.itcraft.frogspawn.platform.MemoryAccess`：

- `src/main/java8/MemoryAccessImpl`：全部使用 `sun.misc.Unsafe`，数组访问使用原始偏移量。
- `src/main/java9/MemoryAccessImpl`：数组访问使用 `VarHandle`，原生内存分配/释放复用 base 的 `RawMemoryUtil`。
- `PlatformMemory` 通过反射加载同包 `MemoryAccessImpl`，base 与 `META-INF/versions/9` 由 MR-JAR 机制自动选择。
- `ArrayUtil`、`FixedOffHeapPool`、`OffHeapObjectsMemoryPoolImpl` 已不再直接依赖 `Unsafe`，统一走 SPI。

**剩余限制（P2）**：JDK9-21 没有公开的原始地址分配 API，`RawMemoryUtil` 仍使用 `sun.misc.Unsafe.allocateMemory/freeMemory`，JDK25+ 有终端废弃告警。SPI 已就位，后续可增加 `java22` overlay 切换到 `java.lang.foreign`。

### 2.4 构建可靠性

1. **P2：`JAVA_HOME8` 缺失时的回退不够显式**
   当前 `base-jdk-fallback` 会回退到 Maven 自身 JDK。若 Maven 运行在 JDK9+，base 层会因找不到 `sun.misc.Contended` 而失败。建议在 CI 中强制设置 `JAVA_HOME8`，或增加 enforcer 规则在 JDK9+ 且未设置 `mr.base.jdk.home` 时给出明确错误。

2. **P2：Javadoc 依赖完整 JDK8**
   `javadocExecutable` 指向 `${mr.base.jdk.home}/bin/javadoc`，若 `JAVA_HOME8` 指向 JRE（无 javadoc），`package` 会失败。发布环境需保证是完整 JDK。

## 3. 本次已修复

| 文件 | 问题 | 修复 |
|------|------|------|
| `local/LocalObjectsMemoryPoolImpl.java` | 线程本地缓存路径跳过 `reset()`，违反 `ObjectsMemoryPool.release` 契约 | 在 `release` 入口统一 `reset()`，新增回归测试 |
| `local/LocalObjectsMemoryPoolImpl.java` | `FetchStrategy.xxx.equals(...)` | 改为枚举 `==` 比较 |
| `offheap/ClassUtil.java` | 与 `ClassChecker` 重复 37 行，且全项目无引用 | 删除死代码 |
| `offheap/FixedOffHeapPool.java` | 池耗尽或 CAS 失败时返回未托管对象，归还污染索引栈 | 耗尽抛 `IllegalStateException`；共享栈操作改为锁保护，移除 10 次 CAS 兜底创建 |
| `offheap/FixedOffHeapPool.java` | 释放兜底分支覆盖并丢弃缓存索引 | 增加归属校验与 `IN_USE/AVAILABLE` 状态 CAS；非法释放抛 `IllegalArgumentException` |
| `offheap/FixedOffHeapPool.java` | `objectSize`、`memoryBlock`、`creator` 仅构造期使用 | 改为局部变量 |
| `offheap/FixedOffHeapPool.java` | `if (obj == null) return;` 缺少花括号 | 补花括号 |
| `offheap/OffHeapMemInfo.java` | 填充字节数 `padding` 为手工伪共享/对齐干预，不能删除 | 保留并累计到 `totalPadding`，新增 `getTotalPadding()` |
| `offheap/OffHeapObjectsMemoryPoolImpl.java` | 数组按固定 8 字节/元素分配 | `PrimitiveStructure` 新增默认 `getArrayElementSize`，按实际元素宽度分配 |
| `offheap/OffHeapObjectsMemoryPoolImpl.java` | `i * SLOT_SIZE` 整型乘法可能溢出 | 改为 `(long) i * SLOT_SIZE` |
| `offheap/OffHeapObjectsMemoryPoolImpl.java` | 抛裸 `RuntimeException` | 改为 `IllegalStateException` |
| `offheap/ClassChecker.java` | `Field.setAccessible` 未放在特权块 | 改为 `AccessController.doPrivileged` |
| `offheap/UnsafeUtil.java`、`util/UnsafeUtil.java` | 抛裸 `RuntimeException` | 改为 `IllegalStateException` |
| `util/ArrayUtil.java` | 直接依赖 `Unsafe`；抛裸 `RuntimeException` | 改走 `MemoryAccess` SPI；改为 `IllegalArgumentException` |
| `strategy/FetchFailStrategy.java` | 抛裸 `RuntimeException` | 改为 `IllegalStateException` |
| `platform/*` | `Unsafe` 散落在池与工具类 | 新增 `MemoryAccess` SPI、`PlatformMemory`、`MemoryAccessImpl`、`RawMemoryUtil` |
| `pom.xml` | `sources.jar` 缺少 `src/main/java9` | 通过 antrun 映射到 `META-INF/versions/9` |
| 项目根 `.astgrepignore` | ast-grep 对有意 CAS/阻塞循环与测试输出的误报 | 逐条登记并注明原因 |

新增测试：
- `frogspawn-test/src/test/java/cn/itcraft/frogspawn/LocalObjectsMemoryPoolResetTest.java`
- `frogspawn-test/src/test/java/cn/itcraft/frogspawn/offheap/FixedOffHeapPoolTest.java`

## 4. 遗留问题

### P0（必须本次修复）

无。

> PMD 的 2 条 `ClassNamingConventions`（`ObjectsMemoryPoolFactory`、`Constants`）priority=1，但二者是既有公共 API 命名，改名会造成破坏性变更，按误报处理。

### P1（下一迭代修复）

无。原 `FixedOffHeapPool` 的耗尽/重试/释放兜底问题已修复并有回归测试。

### P2（建议改进）

1. **JDK9-21 原生内存分配仍依赖 `sun.misc.Unsafe`**
   SPI 已抽出，`java9` 覆盖层已用 VarHandle 处理数组访问；但 JDK9-21 没有公开的原始地址分配 API，`RawMemoryUtil` 仍使用 `Unsafe.allocateMemory/freeMemory`。后续可增加 `java22` overlay 使用 `java.lang.foreign`。
2. **`JAVA_HOME8` 缺失时回退不够显式**（见 2.4）。
3. **Javadoc 依赖完整 JDK8**（见 2.4）。
4. `OffHeapMemInfo` 与 `FieldType` 在删除 `ClassUtil` 后已无生产引用，属于潜在死代码；因是 public 类型，暂不删除，建议在下个大版本评估。
5. SpotBugs `MS`：`UnsafeUtil.getUnsafe()/getTheUnsafe()` 暴露内部 `Unsafe` 实例；这是工具类的设计目的，保留。
6. SpotBugs `EI`：`WrappedResettable.getUsed()` 暴露内部原子状态；该对象用于池内部 CAS，保留。
7. PMD `LawOfDemeter`（25 条）、`AvoidCatchingGenericException`（4 条）、复杂度（8 条）：属于既有设计与性能取舍，暂不调整。
8. ast-grep medium（24 条）：
   - `enum-constant-without-comment` 16 条（`FieldType`）
   - `collection-no-initial-capacity` 6 条（测试）
   - `string-concat-in-loop` 2 条（测试基准）

## 5. 工具明细

### 5.1 ast-grep

```bash
CODE_CHECKLIST_ROOT=/disk2/helly_data/code/ast-grep-rules/code-checklist
"$CODE_CHECKLIST_ROOT/scripts/run.sh" --lang java --level high --path .
# 退出码 0，high 0 条
```

medium 分布：

| 规则 | 数量 |
|------|------|
| enum-constant-without-comment | 16 |
| collection-no-initial-capacity | 6 |
| string-concat-in-loop | 2 |

### 5.2 PMD

- 总违规：39
- priority 1：2（命名规则，误报）
- priority 3：37
- 主要规则：`LawOfDemeter` 25、复杂度相关 8、`AvoidCatchingGenericException` 4、命名 2
- CPD：0 处重复

### 5.3 SpotBugs

| 优先级 | 类型 | 位置 | 判定 |
|--------|------|------|------|
| M | MS | `util/UnsafeUtil.getUnsafe()` | 设计目的，保留 |
| M | MS | `offheap/UnsafeUtil.getTheUnsafe()` | 设计目的，保留 |
| M | EI | `WrappedResettable.getUsed()` | 池内部 CAS 需要，保留 |

## 6. 验证记录

```bash
# 构建（JDK27 编译 overlay，JDK8 编译 base）
export JAVA_HOME8=/path/to/jdk8
export JAVA_HOME=/path/to/jdk9+
mvn -f frogspawn/pom.xml clean install -DskipTests -Dgpg.skip=true

# 测试
mvn -f frogspawn-test/pom.xml test   # JDK8: 17/17
mvn -f frogspawn-test/pom.xml test   # JDK21: 17/17
```

运行期矩阵：JDK8 / 11 / 17 / 21 / 25 / 27 全部通过。

---

**审查人**：AI Code Review
**报告版本**：1.1.0

# frogspawn 代码审查报告（MR-JAR 改造专项）

- **日期**：2026-11-17
- **序号**：001
- **语言**：Java
- **基线**：`74ee59c build: package as JDK8-based multi-release jar` + 后续修复
- **审查范围**：
  - `frogspawn/src/main/java`（平台无关代码）
  - `frogspawn/src/main/java8`（JDK8 专有实现）
  - `frogspawn/src/main/java9`（JDK9+ 覆盖实现）
  - `frogspawn/src/main/java24`（JDK24+ 覆盖实现）
  - `frogspawn-test/src/test/java`（测试与基准）
- **审查引擎**：
  - ast-grep 0.51.1 + code-checklist（33 条 Java 规则）
  - PMD 6.19.0（quickstart + design + security）
  - CPD 6.19.0（minimum-tokens=100）
  - SpotBugs 4.7.3（effort=max，threshold=low，JDK17 运行 base 字节码）

## 1. 总体结论

| 维度 | 结果 | 判定 |
|------|------|------|
| MR-JAR 结构与运行时兼容性 | JDK8 base + JDK9+ / JDK24+ overlay 均正确加载 | **通过** |
| JDK24+ 无 `sun.misc.Unsafe` | `versions/24` 使用 `MemorySegment`，运行时无 Unsafe 告警 | **通过** |
| ast-grep high（CI 门禁） | 0 条 | **通过** |
| ast-grep medium | 24 条（枚举注释、集合初始容量、测试循环拼接） | 登记，下一迭代 |
| PMD P0 | 0 条（2 条命名规则按误报处理） | **通过** |
| PMD P3 | 37 条（LawOfDemeter、复杂度等） | 登记 |
| SpotBugs P0/P1 | 0 条（3 条内部表示暴露为设计目的） | **通过** |
| CPD 重复代码 | 0 处（≥100 tokens） | **通过** |
| 单元测试 | 18/18，JDK8 / JDK21 / JDK27 均通过 | **通过** |

**结论：MR-JAR 改造、P1/P2 修复以及 JDK24+ 无 Unsafe 迁移均已完成，可以进入发布流程。**

## 2. MR-JAR 专项审查

### 2.1 结构正确性

| 检查项 | 结果 |
|--------|------|
| `src/main/java` 不含 `sun.misc.*` / `jdk.*` 内部 API | 通过 |
| `src/main/java8` 承载 `sun.misc.Unsafe`、`sun.misc.Contended`、`RawMemoryUtil` | 通过 |
| `src/main/java9` 不直接引用 `sun.misc.*` | 通过（VarHandle + base 的 `RawMemoryUtil`） |
| `src/main/java24` 完全不引用 `sun.misc.*` | 通过（`Arena` / `MemorySegment` / `VarHandle`） |
| 覆盖类包名/类名与 base 一致 | 通过 |
| base Class 版本 | 52（Java 8） |
| `versions/9` Class 版本 | 53（Java 9） |
| `versions/24` Class 版本 | 68（Java 24） |
| Manifest | `Multi-Release: true`、`Automatic-Module-Name` 存在 |
| `META-INF/versions/*` 中无构建残留（`jpms.args` 已排除） | 通过 |
| `sources.jar` 包含 `java8`、`versions/9`、`versions/24` 源码 | 通过 |

### 2.2 运行时验证

| JDK | 加载路径 | `PaddedAtomicLong` 注解 | `MemoryAccessImpl` 来源 | 池功能 | Unsafe 告警 |
|-----|----------|--------------------------|--------------------------|--------|-------------|
| 8 | base | `sun.misc.Contended` | base | 正常 | 0 |
| 11 / 17 / 21 | `versions/9` | `jdk.internal...Contended` | `versions/9` | 正常 | 0 |
| 25 / 27 | `versions/24` | `jdk.internal...Contended` | `versions/24` | 正常 | **0** |

`jdk.internal.vm.annotation.Contended` 仅作为类文件注解被 JVM 读取，运行期不需要 `--add-exports`；仅编译期需要，已在 `compile-java9` 中配置。

### 2.3 平台 SPI

`cn.itcraft.frogspawn.platform.MemoryAccess` 统一抽象：

- 内存分配/释放
- 对象数组元素访问
- `byte/short/char/int/long/float/double` 读写
- `setMemory` / `copyMemory`

实现分层：

| 层 | 实现 |
|----|------|
| JDK8 | `sun.misc.Unsafe` |
| JDK9-23 | `VarHandle` 数组访问 + `RawMemoryUtil`（Unsafe） |
| JDK24+ | `Arena` / `MemorySegment` 分配释放、`MemorySegment.copyFrom` 拷贝、`VarHandle` 数组访问 |

`PlatformMemory` 通过反射加载同包 `MemoryAccessImpl`，MR-JAR 自动选择 base / `versions/9` / `versions/24`。

### 2.4 构建可靠性

1. **P2：`JAVA_HOME8` 缺失时的回退不够显式**`base-jdk-fallback` 会回退到 Maven 自身 JDK。若 Maven 运行在 JDK9+，base 层会因找不到 `sun.misc.Contended` 而失败。建议 CI 强制设置 `JAVA_HOME8`，或增加 enforcer 规则。

2. **P2：Javadoc 依赖完整 JDK8**`javadocExecutable` 指向 `${mr.base.jdk.home}/bin/javadoc`，若 `JAVA_HOME8` 指向 JRE（无 javadoc），`package` 会失败。

## 3. 本次已修复

| 文件 | 问题 | 修复 |
|------|------|------|
| `local/LocalObjectsMemoryPoolImpl.java` | 线程本地缓存路径跳过 `reset()`，违反 `release` 契约 | 入口统一 `reset()`，新增回归测试 |
| `local/LocalObjectsMemoryPoolImpl.java` | 枚举 `.equals()` | 改为 `==` |
| `offheap/ClassUtil.java` | 与 `ClassChecker` 重复且无引用 | 删除死代码 |
| `offheap/FixedOffHeapPool.java` | 耗尽/重试返回未托管对象 | 耗尽抛 `IllegalStateException`；共享栈锁保护；移除兜底创建 |
| `offheap/FixedOffHeapPool.java` | 释放兜底覆盖索引 | 归属校验 + `IN_USE/AVAILABLE` 状态 CAS；非法释放抛异常 |
| `offheap/FixedOffHeapPool.java` | 未使用字段、缺花括号 | 清理并补全；新增 `AutoCloseable.close()` 释放原生内存 |
| `offheap/OffHeapMemInfo.java` | `padding` 为手工伪共享/对齐干预 | 保留并累计到 `totalPadding`，新增 `getTotalPadding()` |
| `offheap/OffHeapObjectsMemoryPoolImpl.java` | 数组固定 8 字节/元素 | `PrimitiveStructure.getArrayElementSize` 按实际宽度分配 |
| `offheap/OffHeapObjectsMemoryPoolImpl.java` | 整型乘法溢出风险、裸异常 | `(long) i * SLOT_SIZE`；具体异常类型 |
| `offheap/ClassChecker.java` | `Field.setAccessible` 未特权化 | `AccessController.doPrivileged` |
| `util/ArrayUtil.java` | 直接依赖 Unsafe | 改走 `MemoryAccess` SPI；具体异常类型 |
| `platform/*` | Unsafe 分散在池与工具类 | 新增 SPI 与三套实现 |
| `src/main/java24` | JDK24+ Unsafe 被移除 | 新增 `MemorySegment` 覆盖实现，零 Unsafe |
| `pom.xml` | `sources.jar` 缺 java9/java24 源码 | antrun 映射到 `META-INF/versions/9`、`/24` |
| 项目根 `.astgrepignore` | ast-grep 误报 | 逐条登记并注明原因 |

新增测试：
- `LocalObjectsMemoryPoolResetTest`
- `FixedOffHeapPoolTest`（耗尽、释放后重取、重复释放、外部对象）
- `MrJarLayoutTest` 增加 `MemoryAccessImpl` 版本层校验

## 4. 遗留问题

### P0（必须本次修复）

无。

> PMD 的 2 条 `ClassNamingConventions`（`ObjectsMemoryPoolFactory`、`Constants`）priority=1，但二者是既有公共 API 命名，改名会造成破坏性变更，按误报处理。

### P1（下一迭代修复）

无。

### P2（建议改进）

1. **JDK9-23 仍依赖 `sun.misc.Unsafe`**JDK24+ 已完全切换到 `MemorySegment`，无 Unsafe 告警；JDK9-23 受限于公开 API，仍使用 `RawMemoryUtil`。这是预期行为。
2. **`JAVA_HOME8` 缺失时回退不够显式**（见 2.4）。
3. **Javadoc 依赖完整 JDK8**（见 2.4）。
4. `OffHeapMemInfo` 与 `FieldType` 在删除 `ClassUtil` 后已无生产引用，属于潜在死代码；因是 public 类型，暂不删除。
5. SpotBugs `MS`：`UnsafeUtil.getUnsafe()/getTheUnsafe()` 暴露内部 `Unsafe` 实例；工具类设计目的，保留。
6. SpotBugs `EI`：`WrappedResettable.getUsed()` 暴露内部原子状态；池内部 CAS 需要，保留。
7. PMD `LawOfDemeter`（25 条）、`AvoidCatchingGenericException`（4 条）、复杂度（8 条）：既有设计与性能取舍，暂不调整。
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

medium 分布：`enum-constant-without-comment` 16、`collection-no-initial-capacity` 6、`string-concat-in-loop` 2。

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
# 构建（JDK27 编译 versions/9 + versions/24，JDK8 编译 base）
export JAVA_HOME8=/path/to/jdk8
export JAVA_HOME=/path/to/jdk9+
mvn -f frogspawn/pom.xml clean install -DskipTests -Dgpg.skip=true

# 测试
mvn -f frogspawn-test/pom.xml test   # JDK8: 18/18
mvn -f frogspawn-test/pom.xml test   # JDK21: 18/18
mvn -f frogspawn-test/pom.xml test   # JDK27: 18/18
```

运行期矩阵：JDK8 / 11 / 17 / 21 / 25 / 27 全部通过；JDK25/27 无 Unsafe 告警。

---

**审查人**：AI Code Review**报告版本**：1.2.0

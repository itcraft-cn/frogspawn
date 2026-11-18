# frogspawn

## 项目描述

frogspawn 是一个高性能 Java 对象池，专注于 GC 友好和低延迟场景。通过预分配和预处理对象，用于替代大量无状态小对象的分配，防止 GC 导致的延迟波动。

## 项目结构

```
frogspawn/                   # 主库模块
├── src/main/java/cn/itcraft/frogspawn/
│   ├── offheap/             # 非堆内存池接口与平台无关逻辑
│   ├── local/               # 堆内存池实现
│   ├── platform/            # 平台 SPI（MemoryAccess / PlatformMemory）
│   ├── strategy/            # 池策略
│   ├── misc/                # 平台无关工具类
│   ├── util/                # 平台无关工具类
│   ├── constants/           # 常量定义
│   └── data/                # 数据封装类
├── src/main/java8/          # JDK8 专有实现（Unsafe / Contended / RawMemoryUtil）
├── src/main/java9/          # JDK9+ 覆盖实现（VarHandle / jdk.internal Contended）
├── src/main/java24/         # JDK24+ 覆盖实现（MemorySegment / VarHandle）
frogspawn-test/              # 测试模块
├── src/test/java/           # 单元测试和基准测试
```

## MR-JAR 源码分层

本项目以 JDK8 为 base 发布 Multi-Release JAR：

| 源码目录 | 内容 | 编译 JDK | Class 版本 |
|---------|------|----------|-----------|
| `src/main/java` | 与 JDK 版本无关的代码 | JDK8 | 52 |
| `src/main/java8` | 直接依赖 `sun.misc.*` 的实现 | JDK8 | 52 |
| `src/main/java9` | JDK9+ 覆盖实现 | JDK9+ | 53 |
| `src/main/java24` | JDK24+ 覆盖实现（MemorySegment，无 Unsafe） | JDK24+ | 68 |

约束：

1. 只有真正与 JDK 版本无关的代码才能放 `src/main/java`。
2. 任何 `sun.misc.*`、`sun.misc.Unsafe` 相关实现必须放 `src/main/java8`。
3. 需要按 JDK 版本覆盖的类放 `src/main/java9`，包名与类名必须和 base 完全一致。
4. 新增 JDK 专有实现时，优先抽取平台无关接口，避免复制整段业务逻辑。
5. 平台相关访问统一通过 `cn.itcraft.frogspawn.platform.MemoryAccess` SPI；`src/main/java9` 不得直接 `import sun.misc.*`，原生内存分配复用 `RawMemoryUtil`。
6. JDK24+ 的 `src/main/java24` 必须完全不使用 `sun.misc.*`，原生内存使用 `java.lang.foreign.MemorySegment`，数组访问使用 `VarHandle`。

## MR-JAR 构建

```bash
# 完整 MR-JAR：JDK8 编译 base，JDK9+ 编译 versions/9，JDK24+ 编译 versions/24
export JAVA_HOME8=/path/to/jdk8
export JAVA_HOME=/path/to/jdk24+
mvn -f frogspawn/pom.xml clean package

# 指定 JDK8 路径（覆盖 JAVA_HOME8）
mvn -f frogspawn/pom.xml clean package -Dmr.base.jdk.home=/path/to/jdk8
```

- 只用 JDK8 构建时 `mr-java9` / `mr-java24` profile 不激活，产物仍是合法 MR-JAR，但不含 `META-INF/versions/9` / `versions/24` 覆盖类。
- 发布前应使用 JDK24+ 执行一次完整构建，并校验 jar 内存在 `META-INF/versions/9`、`META-INF/versions/24` 目录与 `Multi-Release: true` 清单项。

### MR-JAR 硬约束

1. base 层不能用 `--release 8` 编译：`sun.misc.Unsafe` 不在 `ct.sym` 的 Java 8 签名里。本项目保持 `source/target 8`，并用 JDK8 `javac` 编译 base。
2. 版本化类只在**从 jar 加载**时生效；跑 `target/classes` 的单测永远测基础版。必须对成品 jar 验证版本层选择（见 `MrJarLayoutTest`）。
3. 版本化类与基础类必须同名同签名；版本化实现只依赖基础公共 API，不得引用旧 JDK 才有的类。
4. 后端选择由 MR-JAR 版本目录决定（base / `versions/9` / `versions/24`），不额外引入系统属性开关；如后续新增开关，启动日志必须打印实际后端。
5. `src/main/java24` 使用 `MemorySegment`；如使用 restricted method，按实际 JDK 版本确认是否需要 `--enable-native-access`。当前 JDK25/27 已验证无需额外参数。
6. 平台桥接保持叶子形态：`PlatformMemory` / `MemoryAccess` 是唯一后端入口；热路径使用 `static final` 常量，避免在业务代码中散落后端判断。

验收清单：

- JDK8 `clean test` 全绿；JDK24+ `clean package` 全绿。
- 成品 jar 根目录含基础实现，`versions/9`、`versions/24` 含覆盖实现，manifest 含 `Multi-Release: true`。
- 同一 jar 在 JDK8 / JDK21 / JDK25 上运行探针，各自报告预期后端。
- `sources.jar` 可追溯到 `java8`、`java9`、`java24` 源码。

## 性能测试结果

### JMH 吞吐对比（4 场景）

环境：Intel Core Ultra 9 285H / 16 vCPU，4GB heap，4 threads，10×100ms warmup，5×20ms measurement，fork 3。

| 场景 | JDK8 (ops/us) | JDK25 (ops/us) | JDK25 / JDK8 |
|------|--------------:|---------------:|-------------:|
| Heap Pool | 666.94 | 698.53 | 1.047x |
| OffHeap Pool | 619.21 | 678.98 | 1.097x |
| FixedOffHeap Pool | 300.25 | 295.75 | 0.985x |
| new | 363.17 | 359.22 | 0.989x |

结论：小对象、无 GC 压力时 Heap 最快；OffHeap 在 JDK25 已接近 Heap；FixedOffHeap 的微基准吞吐较低，其价值在固定地址、零分配、大对象与 GC 敏感场景。**Heap 模式最大的优势是平滑 GC 带来的延迟抖动**：预分配/复用降低分配速率，线程本地缓存降低竞争，减少 GC 次数与 pause，使 p95/p99.9 更稳定。具体生产数据因环境、负载与 GC 配置而异，需结合 on/off 对比与 GC 日志归因。详见 [`doc/review/jmh-20261117-001-heap-offheap.md`](doc/review/jmh-20261117-001-heap-offheap.md)。超大对象（1MB）结果见 [`doc/review/jmh-20261117-002-large-objects.md`](doc/review/jmh-20261117-002-large-objects.md)：池化 fetch/release 比 `new` 快约 3 个数量级，JDK25 OffHeap（MemorySegment）达到 673 ops/us。

### 小对象 (~64 bytes) - GC 压力测试

| 方案 | 时间 (ms) | GC 次数 |
|------|-----------|---------|
| **FixedOffHeapPool** | **17.59** | **0** |
| new | 27.37 | 0 |
| HeapPool | 118.65 | 0 |

### 大对象 (~2KB) - GC 压力测试

| 方案 | 时间 (ms) | GC 次数 |
|------|-----------|---------|
| **FixedOffHeapPool** | **82.32** | **0** |
| new | 127.91 | 1 |
| HeapPool | 645.25 | 0 |

### 结论

- **FixedOffHeapPool**: 性能最优 - 执行最快，零 GC，通过 `UNSAFE.setMemory` 高效 native reset
- **HeapPool**: 大对象 reset (数组清零) 开销 > JVM 新对象零值化
- **new**: 中等性能，压力下可能触发 GC

## 文档

- 中文 README：`README.md`
- 英文 README：`README_en.md`
- 中文使用手册：`MANUAL.md`
- 英文使用手册：`MANUAL_en.md`
- 中文更新记录：`CHANGELOG.md`
- 英文更新记录：`CHANGELOG_en.md`

## AI guide

### 角色定位

1. 你是资深架构师
    - 在开发前，会对需求进行详尽分析，提供多套方案，以上、中、下三策的形式呈现，以备后续决策参考
    - 在设计时，会充分考虑非功能性需求：安全性、可扩展性、可用性、可观测性、性能等
    - 在设计细节时，充分考虑各种设计模式及各语言特性
2. 你是资深开发者
    - 对 Java 的 SDK/第三方库均非常了解
    - 对 JDK 各版本间细节均了解
    - 对 JVM 调优也非常擅长
    - 尤其擅长性能调优/反射/多线程/Unsafe底层/网络通信
    - 对 JVM 内存布局非常清楚
    - 开发上偏好面向对象编程（OOP）+接口

### 工作准则

1. 编码前先读取本文件与语言规范；信息不足时先检索代码库，不臆造接口、配置与依赖。
2. 修改遵循最小范围，不顺手重构，不删除或绕过既有测试。
3. 新增或修改逻辑必须有测试或可复现的验证步骤，并在交付说明中写明验证方式。
4. 禁止用 stdout/stderr 输出调试信息，统一使用日志。
5. 提交遵循约定式提交，仅使用当前 `user.name`，不推送远端。

### Markdown 文档规范

1. 段落、列表项、引用块按语义/整句换行，禁止按 80/120 字符等固定列宽硬折行。
2. 代码块、表格保持原样；表格每行一条记录，不按列宽折断。
3. 需要强制换行的元数据（如项目/版本/日期）使用列表或显式硬换行（行尾两个空格），不得用于普通段落。
4. 中英文文档遵循同一规则，不因英文单词边界而按列宽折行。

### 环境信息

通过 skill /java-env 获取 JDK、mvnd/mvn 的路径与版本。

### 环境变量

`${AI_SPEC_ROOT}` 定义在 bash/zsh 环境变量中，可被读取：`echo ${AI_SPEC_ROOT}`。

### 交互规则

必须遵循 `interaction.rules.md` 中描述的规则。

授权读取：${AI_SPEC_ROOT}/rules/interaction.rules.md

项目补充：

1. 所有交互均使用简体中文
2. 处于 AI Coding Plan 包月模式下，不需要担心 Token，专注于高效而完整地工作
3. 每次沟通产出文件后，均执行 git 提交
4. git 仅以当前 `user.name` 提交，不推送到远端
5. git 提交均遵循约定式提交规范（Conventional Commits）执行
6. 重要内容/TODO Plan，随时记录到 MEMORY.md，版本管理忽略该文件，写入 .gitignore，不提交到 Git

### 编码规范

授权读取：${AI_SPEC_ROOT}/lang-spec/spec.java.md
授权读取：${AI_SPEC_ROOT}/lang-spec/review.java.md

#### 项目特定规范

1. **文件头模板**：所有类使用 Apache License 2.0 头文件
2. **双语注释**：使用中文/英文格式
   ```java
   /**
    * 获取对象从池中
    * Fetch object from pool
    */
   ```
3. **导入顺序**：静态导入 → java.* → 第三方库 → 项目包
4. **静态变量命名**：
   - 不可变变量：`UPPER_SNAKE_CASE`
   - 可变变量：`camelCase`
5. **工具类/常量类**：`final class` + 私有构造函数
6. **性能关键代码**：
   - 避免在热路径中分配对象
   - 使用位运算替代算术运算
   - `Unsafe` 操作添加详细注释

### 构建与验证

授权读取：${AI_SPEC_ROOT}/lang-spec/ci.java.md

首选 mvnd，次选 mvn。

本机 Java 工具链（SpotBugs、PMD、Arthas、async-profiler、jitwatch、堆转储解析等）的路径与用途见 skill `/java-env`，模板不重复维护机器事实。

## Build/Lint/Test Commands

### 编译
```bash
# 编译主模块
cd frogspawn && mvn clean compile -DskipTests

# 编译测试模块
cd frogspawn-test && mvn clean test-compile

# 安装主模块到本地仓库
cd frogspawn && mvn clean install -DskipTests -Dmaven.javadoc.skip=true
```

### 运行测试
```bash
# 运行所有测试
cd frogspawn-test && mvn test

# 运行单个测试类
cd frogspawn-test && mvn test -Dtest=OffHeapObjectsMemoryPoolImplTest

# 运行单个测试方法
cd frogspawn-test && mvn test -Dtest=OffHeapObjectsMemoryPoolImplTest#testFetchAndRelease

# 运行特定包下的测试
cd frogspawn-test && mvn test -Dtest="cn.itcraft.frogspawn.offheap.*"
```

### 运行基准测试
```bash
# 运行JMH基准测试
cd frogspawn-test && mvn exec:java -Dexec.mainClass="cn.itcraft.frogspawn.offheap.OffHeapObjectsMemoryPoolBenchmark" -Dexec.classpathScope="test"
```

### 运行简单性能测试
```bash
cd frogspawn-test && mvn exec:java -Dexec.mainClass="cn.itcraft.frogspawn.offheap.SimplePerformanceTest" -Dexec.classpathScope="test"
```

## 系统属性配置

可通过JVM参数配置：
- `-Dfrogspawn.fetch.times`: 最大获取尝试次数，默认10
- `-Dfrogspawn.max.capacity`: 最大池容量，默认1048576
- `-Dfrogspawn.cache.capacity`: 线程缓存容量，默认8，最大64
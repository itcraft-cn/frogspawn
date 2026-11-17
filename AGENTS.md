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
5. 平台相关访问统一通过 `cn.itcraft.frogspawn.platform.MemoryAccess` SPI；
   `src/main/java9` 不得直接 `import sun.misc.*`，原生内存分配复用 `RawMemoryUtil`。
6. JDK24+ 的 `src/main/java24` 必须完全不使用 `sun.misc.*`，原生内存使用
   `java.lang.foreign.MemorySegment`，数组访问使用 `VarHandle`。

## MR-JAR 构建

```bash
# 完整 MR-JAR：JDK8 编译 base，JDK9+ 编译 versions/9，JDK24+ 编译 versions/24
export JAVA_HOME8=/path/to/jdk8
export JAVA_HOME=/path/to/jdk24+
mvn -f frogspawn/pom.xml clean package

# 指定 JDK8 路径（覆盖 JAVA_HOME8）
mvn -f frogspawn/pom.xml clean package -Dmr.base.jdk.home=/path/to/jdk8
```

- 只用 JDK8 构建时 `mr-java9` / `mr-java24` profile 不激活，产物仍是合法 MR-JAR，
  但不含 `META-INF/versions/9` / `versions/24` 覆盖类。
- 发布前应使用 JDK24+ 执行一次完整构建，并校验 jar 内存在
  `META-INF/versions/9`、`META-INF/versions/24` 目录与 `Multi-Release: true` 清单项。

## 性能测试结果

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

### 环境信息

通过 skill /java-env 获取

### 交互规则

1. 所有交互均使用简体中文
2. 处于 AI Coding Plan 包月模式下，不需要担心 Token，专注于高效而完整地工作
3. 每次沟通产出文件后，均执行 git 提交
4. git 仅以当前 `user.name` 提交，不推送到远端
5. git 提交均遵循约定式提交规范（Conventional Commits）执行
6. 重要内容/TODO Plan，随时记录到 MEMORY.md，版本管理忽略该文件，写入 .gitignore，不提交到 Git

### 编码规范

授权读取：/disk2/helly_data/code/markdown/self-ai-spec/lang-spec/spec.java.md

Read /disk2/helly_data/code/markdown/self-ai-spec/lang-spec/spec.java.md

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

### 构建工具

授权读取：/disk2/helly_data/code/markdown/self-ai-spec/lang-spec/ci.java.md

Read /disk2/helly_data/code/markdown/self-ai-spec/lang-spec/ci.java.md

首选 mvnd，次选 mvn

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
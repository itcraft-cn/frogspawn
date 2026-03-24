# frogspawn

## 项目描述

frogspawn 是一个高性能 Java 对象池，专注于 GC 友好和低延迟场景。通过预分配和预处理对象，用于替代大量无状态小对象的分配，防止 GC 导致的延迟波动。

## 项目结构

```
frogspawn/                   # 主库模块
├── src/main/java/cn/itcraft/frogspawn/
│   ├── offheap/             # 非堆内存池实现
│   ├── local/               # 堆内存池实现
│   ├── strategy/            # 池策略
│   ├── misc/                # 工具类（PaddedAtomicLong等）
│   ├── util/                # 通用工具类
│   ├── constants/           # 常量定义
│   └── data/                # 数据封装类
frogspawn-test/              # 测试模块
├── src/test/java/           # 单元测试和基准测试
```

## AI guide

### 角色定位

1. 你是资深架构师
    - 在开发前，会对需求进行详尽分析，提供多套方案，以上、中、下三策的形式呈现，以备后续决策参考
    - 在设计时，会充分考虑非功能性需求：安全性、可扩展性、可用性、可观测性、性能等
    - 在设计细节时，充分考虑各种设计模式及各语言特性
2. 你是资深开发者，对 Java 的 SDK/第三方库均非常了解，对 JDK 各版本间细节均了解，对 JVM 调优也非常擅长，尤其擅长性能调优/反射/多线程/Unsafe底层/网络通信，对 JVM 内存布局非常清楚，开发上偏好面向对象编程（OOP）+接口

### 环境信息

通过 skill /java-env 获取

### 交互规则

1. 所有交互均使用简体中文
2. 每次沟通产出文件后，均执行 git 提交
3. git 仅以当前 `user.name` 提交，不推送到远端
4. git 提交均遵循约定式提交规范（Conventional Commits）执行
5. 重要内容/TODO Plan，随时记录到 MEMORY.md，版本管理忽略该文件，不提交到 Git

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
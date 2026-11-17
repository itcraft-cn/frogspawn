# `frogspawn` 蛙卵

[English version](README.md)

**期望**：

> 如青蛙产卵一般，密集产出大量卵，供后续使用

**目标**：

> 作为内存对象池，预分配预处理

**使用场景**

> 低延迟场合下，用以代替分配大量无状态小对象。防止GC导致的延迟波动。

比如：接口序列化/反序列化时

[SerializeCompareSample](src/test/java/cn/itcraft/frogspawn/sample/serial/SerializeCompareSample.java)

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

`OFF_HEAP` 要求对象实现 `PrimitiveStructure`；`FIXED_OFF_HEAP` 要求实现
`FixedOffHeapObject`。工厂会校验 creator 的返回类型，不匹配时抛出
`IllegalArgumentException`。原有 `newPool` / `newOffHeapPool` / `newFixedOffHeapPool`
方法继续保留，兼容旧代码。

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

- `-Dfrogspawn.fetch.times`，最大循环取次数，默认值：100
- `-Dfrogspawn.max.capacity`，池最大容量，默认值：67108864，即 65536*1024
- `-Dfrogspawn.cache.capacity`, 线程缓存容量, 默认值: 8，最大值: 64。 设置为 `1` 能获得最大性能。

## Java 版本与 MR-JAR

`frogspawn` 以 **Multi-Release JAR** 形式发布，base 层为 JDK8：

| 层 | 源码目录 | Class 版本 | 生效 JDK |
|----|---------|-----------|----------|
| 公共 + JDK8 | `src/main/java` + `src/main/java8` | 52 (Java 8) | JDK8+ |
| JDK9+ 覆盖层 | `src/main/java9` | 53 (Java 9) | JDK9+ |
| JDK24+ 覆盖层 | `src/main/java24` | 68 (Java 24) | JDK24+ |

- `src/main/java`：只放与 JDK 版本无关的代码。
- `src/main/java8`：JDK8 专有实现，包括 `sun.misc.Unsafe`、`sun.misc.Contended`、`RawMemoryUtil`。
- `src/main/java9`：JDK9+ 覆盖实现，包括 `jdk.internal.vm.annotation.Contended` 以及基于
  `VarHandle` 的 `MemoryAccessImpl`。
- `src/main/java24`：JDK24+ 覆盖实现，使用 `java.lang.foreign.MemorySegment`，完全不使用
  `sun.misc.*`。

平台相关访问统一走 `cn.itcraft.frogspawn.platform.MemoryAccess`：
- JDK8 使用 `sun.misc.Unsafe`。
- JDK9-23 使用 `VarHandle` 访问数组，原生内存使用 `RawMemoryUtil`。
- JDK24+ 使用 `MemorySegment` 完成分配/释放、基本类型访问与 `copyFrom`，数组访问使用
  `VarHandle`，JDK24+ 实现不再加载 `sun.misc.Unsafe`。

同时使用 JDK8 与 JDK9+ 构建完整 MR-JAR：

```bash
export JAVA_HOME8=/path/to/jdk8
export JAVA_HOME=/path/to/jdk9+
mvn -f frogspawn/pom.xml clean package
```

可用 `-Dmr.base.jdk.home=/path/to/jdk8` 覆盖 `JAVA_HOME8`。只用 JDK8 构建也能得到
合法的 MR-JAR，只是不含 JDK9+ 覆盖层。

## 伪共享相关

- JDK8 下 base 类使用 `sun.misc.Contended`。
- JDK9+ 下覆盖类使用 `jdk.internal.vm.annotation.Contended`。
- 另有文档说，`@Contended` 注解需要 `JVM` 为用户类开启参数 `-XX:-RestrictContended`。

## 更新记录

[ChangeLog](ChangeLog.md)

# `frogspawn` 蛙卵

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
ObjectsMemoryPool<DemoPojo> pojoPool
        = ObjectsMemoryPoolFactory.newPool(new DemoPojoCreator(), SINGLE_CAPACITY);
```

使用

```java

try{
DemoPojo pojo = pojoPool.fetch();
// TODO using pojo
    ...
            }finally{
            pojoPool.

release(pojo);
}
```

## 三种可选模式

目前支持三种不同模式：

1. 必定从池中取

    ```java
    ObjectsMemoryPool<DemoPojo> pojoPool
        = ObjectsMemoryPoolFactory.newPool(new DemoPojoCreator(), SINGLE_CAPACITY, MUST_FETCH_IN_POOL);
    ```

1. 取一定次数后返空

    ```java
    ObjectsMemoryPool<DemoPojo> pojoPool
        = ObjectsMemoryPoolFactory.newPool(new DemoPojoCreator(), SINGLE_CAPACITY, FETCH_FAIL_AS_NULL);
    ```

1. 取一定次数后创建新对象

    ```java
    ObjectsMemoryPool<DemoPojo> pojoPool
        = ObjectsMemoryPoolFactory.newPool(new DemoPojoCreator(), SINGLE_CAPACITY, FETCH_FAIL_AS_NEW);
    ```

   或

    ```java
    ObjectsMemoryPool<DemoPojo> pojoPool
        = ObjectsMemoryPoolFactory.newPool(new DemoPojoCreator(), SINGLE_CAPACITY);
    ```

默认方案为最后一种。

## 可选参数

- `-Dfrogspawn.fetch.times`，最大循环取次数，默认值：100
- `-Dfrogspawn.max.capacity`，池最大容量，默认值：67108864，即 65536*1024
- `-Dfrogspawn.cache.capacity`, 线程缓存容量, 默认值: 8，最大值: 64。 设置为 `1` 能获得最大性能。

## 伪共享相关

**以下内容需依赖 `JDK8` 下运行，不能高，不能低!!**

[
`@sun.misc.Contended` 源代码](https://github.com/openjdk/jdk/blob/jdk8-b120/jdk/src/share/classes/sun/misc/Contended.java)
里面的注解中说到，对子类可能无效。所以，当前代码中的使用，可能是无效的。

另有文档说，`@Contended` 注解需要 `JVM` 开启参数 `-XX:-RestrictContended` 。

## 更新记录

[ChangeLog](ChangeLog.md)


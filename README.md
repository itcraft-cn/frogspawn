# `frogspawn` 

[中文版本](README_cn.md)

**Concept**:

> Like frogs spawning, densely producing large quantities of eggs for future use

**Objective**:

> Serves as an in-memory object pool with pre-allocation and pre-processing

**Use Cases**

> Used in low-latency scenarios to replace allocation of numerous stateless small objects. Prevents latency fluctuations caused by GC.

Example: During interface serialization/deserialization

[SerializeCompareSample](src/test/java/cn/itcraft/frogspawn/sample/serial/SerializeCompareSample.java)

## Usage

Managed objects need to implement a specific interface `Resettable`

```java
public class DemoPojo implements Resettable {
    ...
}
```

Need to implement a creator

```java
public class DemoPojoCreator implements ObjectCreator<DemoPojo> {
    @Override
    public DemoPojo create() {
        return new DemoPojo();
    }
}
```

Build object pool

```java
PoolStrategy strategy = new PoolStrategy(
        FetchStrategy.MUST_FETCH_IN_POOL, FetchFailStrategy.CALL_CREATOR, true);
ObjectsMemoryPool<DemoPojo> pool =
        ObjectsMemoryPoolFactory.newPool(new DemoPojoCreator(), SIZE, strategy);
```

Usage

```java
try{
    DemoPojo pojo = pojoPool.fetch();
    // TODO using pojo
    ...
}finally{
    pojoPool.release(pojo);
}
```

## Three Available Modes

Currently supports three different modes:

1. Must fetch from pool

    ```java
    PoolStrategy strategy = new PoolStrategy(
        FetchStrategy.MUST_FETCH_IN_POOL, FetchFailStrategy.CALL_CREATOR, true);
    ObjectsMemoryPool<DemoPojo> pool =
        ObjectsMemoryPoolFactory.newPool(new DemoPojoCreator(), SIZE, strategy);
    ```

2. Return null after certain fetch attempts

    ```java
    PoolStrategy strategy = new PoolStrategy(
        FetchStrategy.FETCH_FAIL_AS_NULL, FetchFailStrategy.CALL_CREATOR, true);
    ObjectsMemoryPool<DemoPojo> pojoPool =
        ObjectsMemoryPoolFactory.newPool(new DemoPojoCreator(), SIZE, strategy);
    ```

3. Create new object after certain fetch attempts

    ```java
    PoolStrategy strategy = new PoolStrategy(
        FetchStrategy.FETCH_FAIL_AS_NEW, FetchFailStrategy.CALL_CREATOR, true);
    ObjectsMemoryPool<DemoPojo> pojoPool =
        ObjectsMemoryPoolFactory.newPool(new DemoPojoCreator(), SIZE, strategy);
    ```

The last option is the default.

## Optional Parameters

- `-Dfrogspawn.fetch.times`: Maximum fetch attempts, default: 100
- `-Dfrogspawn.max.capacity`: Maximum pool capacity, default: 67108864 (65536*1024)
- `-Dfrogspawn.cache.capacity`: Thread cache capacity, default: 8, maximum: 64. Setting to `1` provides maximum performance.

## False Sharing Considerations

**The following content requires running on JDK8 specifically - no higher, no lower!!**

[`@sun.misc.Contended` Source Code](https://github.com/openjdk/jdk/blob/jdk8-b120/jdk/src/share/classes/sun/misc/Contended.java)
The annotation mentions it may be ineffective for subclasses. Therefore, current usage in the code might be ineffective.

Additional documentation states that the `@Contended` annotation requires JVM parameter `-XX:-RestrictContended` to be enabled.

## Changelog

[ChangeLog](ChangeLog.md)
```

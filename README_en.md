# `frogspawn`

[Chinese version](README.md) | [Manual](MANUAL_en.md) | [Changelog](CHANGELOG_en.md)

**Concept**:

> Like frogs spawning, densely producing large quantities of eggs for future use

**Objective**:

> Serves as an in-memory object pool with pre-allocation and pre-processing

**Use Cases**

> Used in low-latency scenarios to replace allocation of numerous stateless small objects. Prevents latency fluctuations caused by GC.

Example: During interface serialization/deserialization

[SerializeCompareSample](frogspawn-test/src/test/java/cn/itcraft/frogspawn/sample/serial/SerializeCompareSample.java)

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
        FetchStrategy.MUST_FETCH_IN_POOL, FetchFailStrategy.CALL_CREATOR);
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

## Unified Pool Creation API

`fetch` / `release` are already identical for all pool types. The unified creation entry is:

```java
ObjectsMemoryPool<DemoPojo> pool = ObjectsMemoryPoolFactory.newPool(
        new DemoPojoCreator(), PoolConfig.offHeap(1024));
```

Switch implementations by changing only the config:

```java
PoolConfig.heap(3000);
PoolConfig.offHeap(1024);
PoolConfig.fixedOffHeap(10000, 64);
```

`OFF_HEAP` requires the object to implement `PrimitiveStructure`;
`FIXED_OFF_HEAP` requires `FixedOffHeapObject`. The factory validates the creator return type
and throws `IllegalArgumentException` on mismatch. Existing `newPool` / `newOffHeapPool` /
`newFixedOffHeapPool` methods remain available for backward compatibility.

## Three Available Modes

Currently supports three different modes:

1. Must fetch from pool

    ```java
    PoolStrategy strategy = new PoolStrategy(
        FetchStrategy.MUST_FETCH_IN_POOL, FetchFailStrategy.CALL_CREATOR);
    ObjectsMemoryPool<DemoPojo> pool =
        ObjectsMemoryPoolFactory.newPool(new DemoPojoCreator(), SIZE, strategy);
    ```

2. Return null after certain fetch attempts

    ```java
    PoolStrategy strategy = new PoolStrategy(
        FetchStrategy.FETCH_FAIL_AS_NULL, FetchFailStrategy.CALL_CREATOR);
    ObjectsMemoryPool<DemoPojo> pojoPool =
        ObjectsMemoryPoolFactory.newPool(new DemoPojoCreator(), SIZE, strategy);
    ```

3. Create new object after certain fetch attempts

    ```java
    PoolStrategy strategy = new PoolStrategy(
        FetchStrategy.FETCH_FAIL_AS_NEW, FetchFailStrategy.CALL_CREATOR);
    ObjectsMemoryPool<DemoPojo> pojoPool =
        ObjectsMemoryPoolFactory.newPool(new DemoPojoCreator(), SIZE, strategy);
    ```

The last option is the default.

## Optional Parameters

- `-Dfrogspawn.fetch.times`: Maximum fetch attempts, default: 10
- `-Dfrogspawn.max.capacity`: Maximum pool capacity, default: 1048576
- `-Dfrogspawn.cache.capacity`: Thread cache capacity, default: 8, maximum: 64. Setting to `1` provides maximum performance.

## Java Version & Multi-Release JAR

`frogspawn` is packaged as a **Multi-Release JAR** with a JDK8 base layer:

| Layer | Source root | Class file | Loaded on |
|-------|-------------|------------|-----------|
| Common + JDK8 | `src/main/java` + `src/main/java8` | 52 (Java 8) | JDK8+ |
| JDK9+ overlay | `src/main/java9` | 53 (Java 9) | JDK9+ |
| JDK24+ overlay | `src/main/java24` | 68 (Java 24) | JDK24+ |

- `src/main/java`: only JDK-independent code.
- `src/main/java8`: JDK8-specific implementation, including `sun.misc.Unsafe`, `sun.misc.Contended`
  and `RawMemoryUtil`.
- `src/main/java9`: JDK9+ replacements, including `jdk.internal.vm.annotation.Contended` and a
  `VarHandle`-based `MemoryAccessImpl`.
- `src/main/java24`: JDK24+ replacements using `java.lang.foreign.MemorySegment`; no `sun.misc.*`.

Platform access goes through `cn.itcraft.frogspawn.platform.MemoryAccess`:
- JDK8 uses `sun.misc.Unsafe`.
- JDK9-23 uses `VarHandle` for array access and `RawMemoryUtil` for raw memory.
- JDK24+ uses `MemorySegment` for allocation/free, primitive access and `copyFrom`, with
  `VarHandle` for array access. No `sun.misc.Unsafe` is loaded by the JDK24+ implementation.

Build a full MR-JAR with both JDK8 and JDK24+:

```bash
export JAVA_HOME8=/path/to/jdk8
export JAVA_HOME=/path/to/jdk24+
mvn -f frogspawn/pom.xml clean package
```

`-Dmr.base.jdk.home=/path/to/jdk8` overrides `JAVA_HOME8`. Building with JDK8 alone
still produces a valid MR-JAR, but without the `META-INF/versions/9` and
`META-INF/versions/24` overlays.

## False Sharing Considerations

- On JDK8 the base class uses `sun.misc.Contended`.
- On JDK9+ the versioned class uses `jdk.internal.vm.annotation.Contended`.
- Additional documentation states that the `@Contended` annotation requires JVM parameter
  `-XX:-RestrictContended` to be enabled for user classes.

## Documentation

- [Manual](MANUAL_en.md)
- [Changelog](CHANGELOG_en.md)

## Performance Test Results

### JMH Throughput Comparison (4 scenarios)

Environment: Intel Core Ultra 9 285H / 16 vCPU, 4GB heap, 4 threads, 10×100ms warmup,
5×20ms measurement, fork 3. JDK8 uses the base implementation; JDK25 uses the
`versions/24` `MemorySegment` implementation.

| Scenario | JDK8 (ops/us) | JDK25 (ops/us) | JDK25 / JDK8 |
|----------|--------------:|---------------:|-------------:|
| Heap Pool | 666.94 | 698.53 | 1.047x |
| OffHeap Pool | 619.21 | 678.98 | 1.097x |
| FixedOffHeap Pool | 300.25 | 295.75 | 0.985x |
| new (baseline) | 363.17 | 359.22 | 0.989x |

Takeaways:

- With small objects and no GC pressure, **Heap is fastest**; on JDK25 OffHeap is close
  to Heap (about 97%).
- FixedOffHeap is slower in this microbenchmark; its value is fixed addresses,
  pre-created zero-allocation objects, large objects and GC-sensitive workloads.
- The real home of OffHeap/FixedOffHeap is GC pressure and off-heap memory, not pure
  `fetch`/`release` throughput for 8-byte objects.

### GC Pressure Test (historical)

Test environment: OpenJDK 25, 4GB heap, warmup 100k iterations, test 1M iterations

#### Small Objects (~64 bytes) - GC Pressure Test

| Implementation | Time (ms) | GC Count |
|----------------|-----------|----------|
| **FixedOffHeapPool** | **17.59** | **0** |
| new | 27.37 | 0 |
| HeapPool | 118.65 | 0 |

#### Large Objects (~2KB) - GC Pressure Test

| Implementation | Time (ms) | GC Count |
|----------------|-----------|----------|
| **FixedOffHeapPool** | **82.32** | **0** |
| new | 127.91 | 1 |
| HeapPool | 645.25 | 0 |

#### Conclusion

- **FixedOffHeapPool**: Best performance - fastest execution, zero GC, efficient native reset
  via `MemoryAccess.setMemory` (`UNSAFE.setMemory` on JDK8-23, `MemorySegment.fill` on JDK24+)
- **HeapPool**: Large object reset (array clearing) overhead > JVM new object zero-initialization
- **new**: Medium performance, may trigger GC under pressure

**Recommendation**: Use `FixedOffHeapPool` for large objects and GC-sensitive scenarios.
```

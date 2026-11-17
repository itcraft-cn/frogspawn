# frogspawn Manual

[Chinese version](MANUAL.md) | [README](README_en.md) | [Changelog](CHANGELOG_en.md)

This manual covers object contracts, the unified creation API, pool selection, MR-JAR build/runtime behavior and common issues.

## 1. Requirements

| Purpose | Requirement |
|---------|-------------|
| Runtime | JDK8 or above |
| Full MR-JAR build | JDK8 (base) + JDK24+ (`versions/9` and `versions/24`) |
| Build tool | `mvnd` preferred, `mvn` fallback |

On JDK24+ the runtime loads `META-INF/versions/24`; native memory is managed by `java.lang.foreign.MemorySegment` and `sun.misc.Unsafe` is not loaded.

## 2. Dependency and build

Maven coordinates (current development version):

```xml
<dependency>
    <groupId>cn.itcraft</groupId>
    <artifactId>frogspawn</artifactId>
    <version>0.7-dev</version>
</dependency>
```

Build the full MR-JAR:

```bash
export JAVA_HOME8=/path/to/jdk8
export JAVA_HOME=/path/to/jdk24+
mvn -f frogspawn/pom.xml clean install -DskipTests
```

Override the JDK8 location:

```bash
mvn -f frogspawn/pom.xml clean package -Dmr.base.jdk.home=/path/to/jdk8
```

Run tests:

```bash
cd frogspawn-test
mvn test
```

## 3. Core concepts

| Type | Purpose |
|------|---------|
| `Resettable` | Base contract: `reset()`, `isInvalid()`, `markId/getMarkedId` |
| `ObjectCreator<T>` | Creates object instances |
| `ObjectsMemoryPool<T>` | Unified usage: `fetch()` / `release(T)` |
| `PoolType` | `HEAP` / `OFF_HEAP` / `FIXED_OFF_HEAP` |
| `PoolConfig` | Pool type, capacity, object size and heap strategy |
| `PrimitiveStructure` | Dynamic off-heap object contract |
| `FixedOffHeapObject` | Fixed off-heap object contract |

## 4. Quick start: heap pool

```java
public class DemoPojo implements Resettable {
    private int value;

    @Override
    public void reset() {
        value = 0;
    }

    @Override
    public int getMarkedId() {
        return -1;
    }

    @Override
    public void markId(int id) {
    }
}

ObjectsMemoryPool<DemoPojo> pool = ObjectsMemoryPoolFactory.newPool(
        new ObjectCreator<DemoPojo>() {
            @Override
            public DemoPojo create() {
                return new DemoPojo();
            }
        },
        PoolConfig.heap(3000));

DemoPojo pojo = pool.fetch();
try {
    // use pojo
} finally {
    pool.release(pojo);
}
```

## 5. Unified creation API

`fetch` / `release` are identical for all pool types; only `PoolConfig` changes.

### 5.1 Heap

```java
ObjectsMemoryPool<DemoPojo> pool = ObjectsMemoryPoolFactory.newPool(
        new DemoPojoCreator(), PoolConfig.heap(3000));
```

With an explicit strategy:

```java
PoolStrategy strategy = new PoolStrategy(
        FetchStrategy.FETCH_FAIL_AS_NEW, FetchFailStrategy.CALL_CREATOR);
ObjectsMemoryPool<DemoPojo> pool = ObjectsMemoryPoolFactory.newPool(
        new DemoPojoCreator(), PoolConfig.heap(3000, strategy));
```

### 5.2 Dynamic off-heap

```java
ObjectsMemoryPool<MyOffHeapPojo> pool = ObjectsMemoryPoolFactory.newPool(
        MyOffHeapPojo::new, PoolConfig.offHeap(1024));
```

`MyOffHeapPojo` must implement `PrimitiveStructure`.

### 5.3 Fixed off-heap

```java
ObjectsMemoryPool<MyFixedPojo> pool = ObjectsMemoryPoolFactory.newPool(
        MyFixedPojo::new, PoolConfig.fixedOffHeap(10000, 64));
```

`MyFixedPojo` must implement `FixedOffHeapObject`; `64` is the byte size per object.

### 5.4 Legacy API

The following methods are kept and delegate to the unified entry:

```java
newPool(creator, size)
newPool(creator, size, strategy)
newOffHeapPool(creator)
newOffHeapPool(creator, maxPoolSize)
newFixedOffHeapPool(creator, capacity, objectSize)
```

## 6. Object contracts

### 6.1 Resettable

- `reset()` is called automatically by `release`.
- `reset()` must not clear pool-managed identity fields:
  - Heap pool `markId` (set by the pool for return lookup);
  - Fixed off-heap `index` / `address` (set by the pool; clearing them breaks the object).
- If `isInvalid()` returns `true`, the heap pool drops the cached object and fetches from the main pool again.

### 6.2 PrimitiveStructure (dynamic off-heap)

Required methods:

- `getAddress()` / `setAddress(long)`: the pool writes back the allocated address.
- `getObjectSize()`: object body size in bytes.
- `getArrayFieldCount()` / `getArrayLength(int)` / `getArrayAddress(int)` / `setArrayAddress(int, long)`: array field management.
- `getArrayElementSize(int)`: element width; defaults to 8 bytes. Return 4 for `int[]`, 1 for `byte[]`.
- `writeToMemory()` / `readFromMemory()`: copy between object and native memory.
- `hasDataToWrite()`: return `false` to skip writing.

Array memory is allocated by the pool as `length × elementSize`; do not allocate it again in the implementation.

### 6.3 FixedOffHeapObject (fixed off-heap)

Required methods:

- `index()` / `index(int)`: fixed index assigned by the pool.
- `address()` / `address(long)`: fixed address assigned by the pool.
- `reset()`: reset business fields only; **do not** clear `index` / `address`.

`FixedOffHeapPool` pre-creates all objects and owns one native block; object addresses are fixed for the pool lifetime. The pool is `AutoCloseable`; close it when done:

```java
ObjectsMemoryPool<MyFixedPojo> pool = ObjectsMemoryPoolFactory.newPool(
        MyFixedPojo::new, PoolConfig.fixedOffHeap(10000, 64));
try {
    // fetch / release
} finally {
    ((AutoCloseable) pool).close();
}
```

After `close()`, `fetch/release` throws `IllegalStateException`.

#### Zero-Copy ByteBuffer View

`FixedOffHeapObject.buffer()` throws `UnsupportedOperationException` by default; implementations may override it:

- **JDK24+**: use `PlatformMemory.asByteBuffer(address(), OBJECT_SIZE)`, backed by `MemorySegment.asByteBuffer()`. It can be passed directly to `FileChannel` / `SocketChannel`:

  ```java
  @Override
  public ByteBuffer buffer() {
      return PlatformMemory.asByteBuffer(address(), OBJECT_SIZE);
  }
  ```

- **JDK8-23**: there is no public API to wrap an arbitrary address as a `ByteBuffer`; the implementation should hold a `ByteBuffer.allocateDirect(OBJECT_SIZE)` and return it from `buffer()`, associating `address()` with that buffer.

Notes:

- Each fetched object/ByteBuffer must be used exclusively; do not share it across threads.
- `position/limit` are ByteBuffer state; call `clear()` / `flip()` as needed before IO.
- Do not use a buffer view after `FixedOffHeapPool.close()`.

## 7. Pool strategies

`FetchStrategy`:

| Strategy | Behavior |
|----------|----------|
| `MUST_FETCH_IN_POOL` | Block until an object is fetched from the pool |
| `FETCH_FAIL_AS_NULL` | Retry `frogspawn.fetch.times`, then return `null` |
| `FETCH_FAIL_AS_NEW` | Retry `frogspawn.fetch.times`, then call the creator (default) |

`FetchFailStrategy`: `NOT_AVAILABLE` / `NULLABLE` / `CALL_CREATOR`.

Strategies currently apply to the heap pool only; off-heap and fixed pools create a new object or throw `IllegalStateException` according to their own semantics.

## 8. System properties

| Property | Default | Description |
|----------|---------|-------------|
| `-Dfrogspawn.fetch.times` | `10` | Maximum fetch attempts |
| `-Dfrogspawn.max.capacity` | `1048576` | Maximum heap pool capacity |
| `-Dfrogspawn.cache.capacity` | `8` | Thread-local cache size, max `64`; `1` gives the highest performance |

For the `@Contended` annotation to affect user classes, usually also enable:

```bash
-XX:-RestrictContended
```

## 9. MR-JAR build and runtime

Source layout:

| Directory | Content | Class file | Runtime |
|-----------|---------|-----------|---------|
| `src/main/java` | JDK-independent code | 52 | JDK8+ |
| `src/main/java8` | Unsafe / Contended / RawMemoryUtil | 52 | JDK8+ |
| `src/main/java9` | VarHandle / `jdk.internal` Contended | 53 | JDK9+ |
| `src/main/java24` | MemorySegment / VarHandle | 68 | JDK24+ |

The JVM selects the highest available version:

- JDK8: base (Unsafe)
- JDK9-23: `META-INF/versions/9` (VarHandle + RawMemoryUtil)
- JDK24+: `META-INF/versions/24` (MemorySegment, no Unsafe)

The `sources.jar` includes `java8`, `versions/9` and `versions/24` sources.

## 10. Performance and selection

JMH (4 threads, 10×100ms warmup, 5×20ms measurement, fork 3):

| Scenario | JDK8 (ops/us) | JDK25 (ops/us) | JDK25 / JDK8 |
|----------|--------------:|---------------:|-------------:|
| Heap Pool | 666.94 | 698.53 | 1.047x |
| OffHeap Pool | 619.21 | 678.98 | 1.097x |
| FixedOffHeap Pool | 300.25 | 295.75 | 0.985x |
| new | 363.17 | 359.22 | 0.989x |

Selection guidance:

- Small objects, short lifetime, throughput first: **Heap**.
- Large objects, large capacity, GC-sensitive, tail-latency-sensitive: **OffHeap**.
- Fixed-size records, stable addresses, zero allocation, native/network/serialization interop: **FixedOffHeap**.
- FixedOffHeap has lower microbenchmark throughput than Heap/OffHeap; that is the cost of fixed addresses and correctness validation. Its value is GC pressure, fixed addresses and zero allocation.

### Heap Mode's Biggest Advantage: Smoothing GC-Induced Latency Jitter

The biggest advantage of Heap mode is not raw `fetch/release` throughput, but **smoothing GC-induced latency jitter**:

- pre-allocation + reuse reduce the allocation rate;
- thread-local caches reduce shared-pool contention;
- fewer GC cycles and shorter pauses make p95 / p99.9 more stable.

In a low-latency system with a fixed thread pool and a fixed processing pipeline, this "variance optimization" is often more valuable than "mean optimization".

Production figures vary with environment, workload and GC configuration; attribute frogspawn's contribution via GC count/pause, allocation rate, and the correlation between p99.9 and GC pauses.

See the [JMH report](doc/review/jmh-20261117-001-heap-offheap.md) for details.

## 11. Troubleshooting

### 11.1 FixedOffHeapPool exhausted

The fixed pool throws `IllegalStateException` when exhausted. Increase the capacity in `PoolConfig.fixedOffHeap(capacity, ...)`, or ensure every `fetch()` has a matching `release()`.

### 11.2 Object does not belong to this pool

`release` only accepts objects created by the same pool and rejects double releases. Check for returning an object to the wrong pool or calling `release` twice.

### 11.3 Objects contain stale data

- Heap: verify `reset()`; `release` calls it.
- FixedOffHeap: verify `reset()` only clears business fields, not `index` / `address`.
- OffHeap: verify `readFromMemory()` is called before reading business data.

### 11.4 Unsafe warnings on JDK24+

JDK24+ should load `META-INF/versions/24`. Check with:

```bash
java -verbose:class -cp frogspawn.jar YourMain 2>&1 | grep MemoryAccessImpl
```

You should see `MemoryAccessImpl$Allocation` (the MemorySegment implementation). If the base implementation is loaded, verify the jar manifest contains `Multi-Release: true`.

## 12. Related documents

- [README](README_en.md)
- [Changelog](CHANGELOG_en.md)
- [JMH benchmark report](doc/review/jmh-20261117-001-heap-offheap.md)
- [Code review report](doc/review/code-review-20261117-001-java.md)

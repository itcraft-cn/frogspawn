# Changelog

[中文版本](CHANGELOG.md)

## v0.7 (in development)

### Build
- Reworked into a **Multi-Release JAR** based on JDK8:
  - base layer compiled with JDK8 (`src/main/java` + `src/main/java8`)
  - JDK9+ overlay emitted to `META-INF/versions/9`
  - JDK24+ overlay emitted to `META-INF/versions/24`
  - manifest now contains `Multi-Release: true` and `Automatic-Module-Name`
- Source layout split by JDK dependency:
  - `src/main/java`: JDK-independent code
  - `src/main/java8`: JDK8-specific implementation (`sun.misc.Unsafe`, `sun.misc.Contended`)
  - `src/main/java9`: JDK9+ overlay (`jdk.internal.vm.annotation.Contended`, `VarHandle`)
  - `src/main/java24`: JDK24+ overlay (`java.lang.foreign.MemorySegment`, no `sun.misc.*`)
- Fixed the JDK9+ compile failure caused by `sun.misc.Contended`
- Aligned `frogspawn-test` dependency to `0.7-dev`

### Fixes and cleanup
- Fixed the release semantics bug where the thread-local cache path skipped `reset()`
- Removed the unused duplicated `ClassUtil`
- Fixed unused fields/locals, integer-multiplication overflow risk and raw exception types
- Added `.astgrepignore` entries for intentional CAS/blocking loops and test output
- Added `LocalObjectsMemoryPoolResetTest`

### Off-heap memory and platform SPI
- Added the `cn.itcraft.frogspawn.platform.MemoryAccess` platform SPI:
  - `java8` uses `sun.misc.Unsafe`
  - `java9` uses `VarHandle` for array access and reuses `RawMemoryUtil`
  - `java24` uses `java.lang.foreign.MemorySegment` for allocation/free, primitive access and `copyFrom`, with `VarHandle` for array access, and uses no `sun.misc.*`
  - `ArrayUtil`, `FixedOffHeapPool` and `OffHeapObjectsMemoryPoolImpl` no longer depend on `Unsafe` directly
- Added the `META-INF/versions/24` overlay
- Added `FixedOffHeapObject.buffer()` (unsupported by default) and `PlatformMemory.asByteBuffer(address, size)`; on JDK24+ it creates a zero-copy `ByteBuffer` view via `MemorySegment.asByteBuffer()`
- `FixedOffHeapPool` fixes:
  - exhaustion/retry no longer returns an unmanaged object; it throws `IllegalStateException`
  - ownership validation and `IN_USE/AVAILABLE` state CAS reject duplicate/foreign releases
  - shared-stack operations are lock-protected to remove the copy/overwrite race
- `PrimitiveStructure` adds a default `getArrayElementSize`; arrays are allocated by actual element width
- `OffHeapMemInfo` keeps the manually tuned padding byte count and accumulates it into `totalPadding`
- `ClassChecker` moves `Field.setAccessible` into `AccessController.doPrivileged`
- `sources.jar` now maps `src/main/java9` and `src/main/java24` into `META-INF/versions/9` and `META-INF/versions/24`
- Added `FixedOffHeapPoolTest` and `UnifiedPoolApiTest` regression tests

### API
- Added the unified pool creation entry:
  - `PoolType`: `HEAP` / `OFF_HEAP` / `FIXED_OFF_HEAP`
  - `PoolConfig`: `heap(...)` / `offHeap(...)` / `fixedOffHeap(...)`
  - `ObjectsMemoryPoolFactory.newPool(creator, PoolConfig)`
- Existing factory methods are kept and delegate to the unified entry
- `fetch` / `release` remain identical for all pool types; off-heap types validate the object interface at creation time

### Performance
- Added the unified JMH benchmark `AllPoolsBenchmark` with four scenarios: Heap Pool / OffHeap Pool / FixedOffHeap Pool / `new`
- JDK8 vs JDK25 (4 threads, 10×100ms warmup, 5×20ms measurement, fork 3):

| Scenario | JDK8 (ops/us) | JDK25 (ops/us) | JDK25 / JDK8 |
|----------|--------------:|---------------:|-------------:|
| Heap Pool | 666.94 | 698.53 | 1.047x |
| OffHeap Pool | 619.21 | 678.98 | 1.097x |
| FixedOffHeap Pool | 300.25 | 295.75 | 0.985x |
| new | 363.17 | 359.22 | 0.989x |

- The JDK25 `versions/24` MemorySegment implementation shows no regression; OffHeap is close to Heap.
- FixedOffHeap has lower throughput in this microbenchmark; its value is fixed addresses, zero allocation, large objects and GC-sensitive workloads.
- Added a 1MB large-object JMH benchmark: pooled fetch/release is about three orders of magnitude faster than `new`; JDK25 OffHeap (MemorySegment) reaches 673 ops/us. See `doc/review/jmh-20261117-002-large-objects.md`.

### Documentation
- Split README into Chinese `README.md` and English `README_en.md`
- Split changelog into Chinese `CHANGELOG.md` and English `CHANGELOG_en.md`
- Added Chinese `MANUAL.md` and English `MANUAL_en.md`
- Documented that Heap mode's biggest advantage is smoothing GC-induced latency jitter; production figures are environment/workload dependent and intentionally not published

## v0.6

### Performance
- **SimpleStackCache replaced SoftRefStore**: O(1) fetch/release, significant throughput improvement (~479%)
- **Removed prefetch**: simplified pool implementation and reduced complexity

### Refactoring
- Simplified `ObjectsMemoryPoolImpl`
- Simplified `PoolStrategy` by removing the prefetch parameter

### Tests
- Added `MultiObjectsMemoryPoolBenchmark`
- Added `run_bench.sh` for JMH benchmarks

### Documentation
- Added the English README
- Added the Java review report `doc/review/java-review-20260326-001.md`
- Review covers design patterns, thread safety, performance and coding standards

### Build
- Version bumped to 0.6

## v0.5

- Refactor; a single pool implementation.

## v0.4

- First release.

## v0.3

- Added `isInvalid` checks.

## v0.2

- Added the auto-fill mode for one-thread-consume / one-thread-return scenarios.

## v0.1

- Initial release.

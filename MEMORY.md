# 性能优化记录

## 基准测试结果（优化前）

**日期**: 2026-03-24 15:52
**测试**: SimplePerformanceTest (1M iterations, single thread)

```
非堆内存池: 65,729,681 ops/sec (20.07% of new)
堆内存池:   81,289,340 ops/sec (24.82% of new)
直接创建:  327,579,616 ops/sec (100.00%)
```

## 优化后性能测试结果

**日期**: 2026-03-24 15:59
**测试**: SimplePerformanceTest (1M iterations, single thread)

```
非堆内存池: 55,965,468 ops/sec (16.22% of new)
堆内存池:  110,623,727 ops/sec (32.05% of new)
直接创建:  345,117,842 ops/sec (100.00%)
```

## 优化效果

| 方案 | 优化前 | 优化后 | 变化 |
|------|--------|--------|------|
| 堆方案 | 81.3M ops/sec | 110.6M ops/sec | **+36%** |
| 非堆方案 | 65.7M ops/sec | 56.0M ops/sec | -15% |

**堆方案显著提升**：SimpleStackCache 替代 SoftRefStore，O(n) → O(1)

**非堆方案略有下降**：数组地址缓存增加了额外开销，但对有数组字段的对象会有收益

## 优化内容

### Phase 1: 堆方案优化
- [x] 重构 SoftRefStore 为 SimpleStackCache (O(1))
- [x] 更新 LocalObjectsMemoryPoolImpl 引用

### Phase 2: 非堆方案优化
- [x] 添加数组地址缓存到 ThreadLocalCache
- [x] 添加 destroy() 方法释放资源
- [x] 配置化支持 (frogspawn.offheap.* JVM参数)

### Phase 3: 验证
- [x] 运行单元测试 - 全部通过 (11 tests)
- [x] 运行性能基准测试对比

## 新增配置参数

非堆内存池可通过以下JVM参数配置：
- `-Dfrogspawn.offheap.pool.size`: 内存池大小，默认 4MB
- `-Dfrogspawn.offheap.slot.size`: 槽位大小，默认 64字节
- `-Dfrogspawn.offheap.batch.size`: 批量操作大小，默认 64
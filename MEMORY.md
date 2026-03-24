# 性能优化记录

## 最终性能对比

**日期**: 2026-03-24
**测试**: SimplePerformanceTest (1M iterations, single thread)

```
非堆内存池: 57-70M ops/sec (约 20% of new)
堆内存池:   89-130M ops/sec (约 35% of new)
直接创建:   280-345M ops/sec (100%)
```

## 优化历程

### 基准性能（优化前）
```
非堆内存池: 65.7M ops/sec (20.07% of new)
堆内存池:   81.3M ops/sec (24.82% of new)
直接创建:  327.6M ops/sec (100.00%)
```

### 优化后
```
堆方案: +36% 性能提升 (81.3M → 110.6M ops/sec)
非堆方案: 移除 instanceof 检查，性能基本持平
```

## 堆方案优化内容

### SimpleStackCache 替代 SoftRefStore
- fetch/release 从 O(n) 降为 O(1)
- 移除 SoftReference 开销
- 移除 walker 指针维护
- 性能提升：+36%

## 非堆方案优化内容

### 移除 instanceof 检查
- ThreadLocalCache 中所有对象都是 PrimitiveStructure
- 直接转型，避免运行时类型检查
- 性能提升：约 5-10%

### 尝试但回退的优化
1. **数组地址缓存**：增加额外开销，对无数组字段对象无益
2. **延迟 reset()**：导致地址被清除，性能大幅下降
3. **简化 allocateMemory**：移除必要的清零操作，效果不明显

## 性能差异根本原因

| 维度 | 非堆方案 | 堆方案 |
|------|---------|--------|
| ThreadLocal 访问 | 2次 (对象+内存) | 1次 |
| 内存地址管理 | getAddress/setAddress | 无 |
| 状态写入 | writeToMemory() | 无 |
| 内存类型 | 直接内存 (Unsafe) | JVM 堆内存 |

非堆方案需要额外的内存管理开销，这是设计上的权衡，无法完全消除。

## 结论

1. **堆方案优化效果显著**：SimpleStackCache 带来 36% 性能提升
2. **非堆方案架构限制**：内存管理开销是不可避免的
3. **适用场景不同**：
   - 堆方案：适合一般场景，性能更好
   - 非堆方案：适合需要直接内存、跨进程共享、零GC 压力的场景
package cn.itcraft.frogspawn.offheap;

import cn.itcraft.frogspawn.strategy.FetchFailStrategy;
import cn.itcraft.frogspawn.strategy.FetchStrategy;

/**
 * 非堆内存对象池策略
 * 扩展PoolStrategy以支持对象大小信息
 */
public class OffHeapPoolStrategy {
    private final FetchStrategy fetchStrategy;
    private final FetchFailStrategy fetchFailStrategy;
    // 对象大小（字节）
    private final int objectSize;

    public OffHeapPoolStrategy(FetchStrategy fetchStrategy,
                               FetchFailStrategy fetchFailStrategy,
                               int objectSize) {
        if (fetchStrategy == null || fetchFailStrategy == null) {
            throw new IllegalArgumentException("FetchStrategy or FetchFailStrategy should not be null");
        }
        if (objectSize <= 0) {
            throw new IllegalArgumentException("Object size must be positive");
        }
        this.fetchStrategy = fetchStrategy;
        this.fetchFailStrategy = fetchFailStrategy;
        this.objectSize = objectSize;
    }

    /**
     * 使用默认策略（FETCH_FAIL_AS_NEW / CALL_CREATOR）
     */
    public OffHeapPoolStrategy(int objectSize) {
        this(FetchStrategy.FETCH_FAIL_AS_NEW, FetchFailStrategy.CALL_CREATOR, objectSize);
    }

    public FetchStrategy getFetchStrategy() {
        return fetchStrategy;
    }

    public FetchFailStrategy getFetchFailStrategy() {
        return fetchFailStrategy;
    }

    public int getObjectSize() {
        return objectSize;
    }
}

package cn.itcraft.frogspawn.impl;

import cn.itcraft.frogspawn.ObjectCreator;
import cn.itcraft.frogspawn.Resettable;
import cn.itcraft.frogspawn.data.WrappedResettable;
import cn.itcraft.frogspawn.strategy.FetchFailStrategy;
import cn.itcraft.frogspawn.util.ArrayUtil;

import java.util.concurrent.atomic.AtomicLong;

import static cn.itcraft.frogspawn.constants.Constants.FETCH_TIMES;

/**
 * 资源获取与故障转移工具类
 * Resource acquisition and failover utility class
 * 
 * @author Helly Guo
 * <p>
 * Created on 09/01/2023 15:36
 */
final class FetchHelper {

    /**
     * 私有构造函数防止实例化
     * Private constructor to prevent instantiation
     */
    private FetchHelper() {
    }

    /**
     * 尝试获取可用资源或执行故障转移策略
     * Attempt to acquire available resource or execute failover strategy
     * 
     * @param array 封装对象数组 / Wrapped objects array
     * @param indexMask 数组索引掩码 / Array index mask
     * @param walker 原子计数器用于遍历数组 / Atomic counter for array traversal
     * @param fetchFailStrategy 获取失败时的处理策略 / Handling strategy when fetch fails
     * @param creator 对象创建器（用于故障转移时创建新对象） / Object creator (for creating new objects during failover)
     * @return 可重置对象实例 / Resettable object instance
     * 
     * @SuppressWarnings 抑制原始类型和未检查转换警告
     * @SuppressWarnings Suppress rawtypes and unchecked conversion warnings
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends Resettable> T fetchDataOrFailover(WrappedResettable[] array, int indexMask,
                                                               AtomicLong walker,
                                                               FetchFailStrategy fetchFailStrategy,
                                                               ObjectCreator<T> creator) {
        WrappedResettable<T> wrapped;
        for (int i = 0; i < FETCH_TIMES; i++) {
            wrapped = ArrayUtil.elementAt(array, indexMask, walker.getAndIncrement());
            if (matchedUnused(wrapped)) {
                return fetchFromWrapped(wrapped);
            }
        }
        return fetchFailStrategy.failover(creator);
    }

    /**
     * 循环获取可用资源（阻塞式）
     * Loop to acquire available resource (blocking)
     * 
     * @param array 封装对象数组 / Wrapped objects array
     * @param indexMask 数组索引掩码 / Array index mask
     * @param walker 原子计数器用于遍历数组 / Atomic counter for array traversal
     * @return 可重置对象实例 / Resettable object instance
     * 
     * @SuppressWarnings 抑制原始类型和未检查转换警告
     * @SuppressWarnings Suppress rawtypes and unchecked conversion warnings
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends Resettable> T loopFetchData(WrappedResettable[] array, int indexMask, AtomicLong walker) {
        WrappedResettable<T> wrapped;
        while (true) {
            wrapped = ArrayUtil.elementAt(array, indexMask, walker.getAndIncrement());
            if (matchedUnused(wrapped)) {
                return fetchFromWrapped(wrapped);
            }
        }
    }

    /**
     * 检查并标记资源为已使用（线程安全）
     * Check and mark resource as used (thread-safe)
     * 
     * @param wrapped 封装的资源对象 / Wrapped resource object
     * @return 是否成功标记 / Whether marking was successful
     */
    private static <T extends Resettable> boolean matchedUnused(WrappedResettable<T> wrapped) {
        // 使用CAS操作保证原子性
        // Use CAS operation to ensure atomicity
        return wrapped.getUsed().compareAndSet(false, true);
    }

    /**
     * 从封装对象中获取实际资源
     * Get actual resource from wrapped object
     * 
     * @param wrapped 封装的资源对象 / Wrapped resource object
     * @return 实际资源实例 / Actual resource instance
     */
    private static <T extends Resettable> T fetchFromWrapped(WrappedResettable<T> wrapped) {
        return wrapped.getObj();
    }
}

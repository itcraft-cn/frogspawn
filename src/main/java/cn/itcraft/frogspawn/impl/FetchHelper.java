package cn.itcraft.frogspawn.impl;

import cn.itcraft.frogspawn.ObjectCreator;
import cn.itcraft.frogspawn.Resettable;
import cn.itcraft.frogspawn.data.WrappedResettable;
import cn.itcraft.frogspawn.strategy.FetchFailStrategy;
import cn.itcraft.frogspawn.util.ArrayUtil;

import java.util.concurrent.atomic.AtomicLong;

import static cn.itcraft.frogspawn.constants.Constants.FETCH_TIMES;

/**
 * @author Helly Guo
 * <p>
 * Created on 09/01/2023 15:36
 */
final class FetchHelper {

    private FetchHelper() {
    }

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

    private static <T extends Resettable> boolean matchedUnused(WrappedResettable<T> wrapped) {
        return wrapped.getUsed().compareAndSet(false, true);
    }

    private static <T extends Resettable> T fetchFromWrapped(WrappedResettable<T> wrapped) {
        return wrapped.getObj();
    }
}

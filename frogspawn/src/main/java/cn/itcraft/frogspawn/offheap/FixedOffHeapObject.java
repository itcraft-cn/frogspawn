package cn.itcraft.frogspawn.offheap;

import cn.itcraft.frogspawn.Resettable;

import java.nio.ByteBuffer;

/**
 * 固定非堆对象接口
 * Fixed off-heap object interface
 * 
 * 用于固定对象池，对象预创建且索引固定
 * Used for fixed object pool, objects are pre-created with fixed indices
 *
 * @author Helly Guo
 * Created on 2026-03-24
 */
public interface FixedOffHeapObject extends Resettable {

    /**
     * 获取对象索引（固定不变）
     * Get object index (fixed)
     *
     * @return 对象索引 / Object index
     */
    int index();

    /**
     * 设置对象索引
     * Set object index
     *
     * @param idx 对象索引 / Object index
     */
    void index(int idx);

    /**
     * 获取内存地址
     * Get memory address
     *
     * @return 非堆内存地址 / Off-heap memory address
     */
    long address();

    /**
     * 设置内存地址
     * Set memory address
     *
     * @param addr 非堆内存地址 / Off-heap memory address
     */
    void address(long addr);

    /**
     * 返回对象的直接 ByteBuffer 视图，用于零拷贝磁盘/网络 IO。
     * Returns a direct ByteBuffer view for zero-copy disk/network IO.
     *
     * 默认实现不支持；实现类可按需覆盖。
     * JDK24+ 可使用 {@link cn.itcraft.frogspawn.platform.PlatformMemory#asByteBuffer(long, long)}
     * 基于 {@code MemorySegment.asByteBuffer()} 创建视图。
     * JDK8-23 没有公开 API 将任意地址包装为 ByteBuffer，建议由实现类持有
     * {@link ByteBuffer#allocateDirect(int)} 并直接返回。
     * <p>
     * The default implementation is unsupported; implementations may override it.
     * On JDK24+ use {@link cn.itcraft.frogspawn.platform.PlatformMemory#asByteBuffer(long, long)}
     * backed by {@code MemorySegment.asByteBuffer()}. On JDK8-23 there is no public API to wrap
     * an arbitrary address; implementations should hold a direct ByteBuffer instead.
     *
     * @return 直接 ByteBuffer 视图 / direct ByteBuffer view
     * @throws UnsupportedOperationException 默认不支持 / unsupported by default
     */
    default ByteBuffer buffer() {
        throw new UnsupportedOperationException("buffer view is not supported");
    }
}
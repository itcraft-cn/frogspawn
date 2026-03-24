package cn.itcraft.frogspawn.offheap;

import cn.itcraft.frogspawn.Resettable;

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
}
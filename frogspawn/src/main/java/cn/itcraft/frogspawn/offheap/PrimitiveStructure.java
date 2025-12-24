package cn.itcraft.frogspawn.offheap;

import cn.itcraft.frogspawn.Resettable;

/**
 * @author Helly Guo
 * <p>
 * Created on 2025-03-21 16:26
 */
public interface PrimitiveStructure extends Resettable {

    /**
     * 获取对象在非堆内存中的地址
     * @return 非堆内存地址
     */
    long getAddress();

    /**
     * 设置对象在非堆内存中的地址
     * @param address 非堆内存地址
     */
    void setAddress(long address);
}

package cn.itcraft.frogspawn.offheap;

import cn.itcraft.frogspawn.Resettable;

/**
 * 基本类型结构接口，用于非堆内存存储
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
    
    /**
     * 获取对象内存大小（字节）
     * 包括对象主体和所有数组字段的指针（但不包括数组数据）
     */
    int getObjectSize();
    
    /**
     * 获取数组字段数量
     */
    int getArrayFieldCount();
    
    /**
     * 获取指定数组字段的地址
     * @param index 数组字段索引（0-based）
     */
    long getArrayAddress(int index);
    
    /**
     * 设置指定数组字段的地址
     * @param index 数组字段索引（0-based）
     * @param address 数组内存地址
     */
    void setArrayAddress(int index, long address);
    
    /**
     * 获取指定数组字段的长度
     * @param index 数组字段索引（0-based）
     */
    int getArrayLength(int index);
    
    /**
     * 将对象数据写入非堆内存
     * 应该将基本类型字段写入对象内存，数组字段写入单独分配的内存
     */
    void writeToMemory();
    
    /**
     * 从非堆内存读取对象数据
     * 应该从对象内存读取基本类型字段，从数组内存读取数组数据
     */
    void readFromMemory();
    
    /**
     * 检查对象是否有数据需要写入内存
     * 用于优化：空实现可以跳过 writeToMemory() 调用
     * 
     * Check if object has data to write to memory
     * For optimization: empty implementations can skip writeToMemory() call
     * 
     * @return true-需要写入内存 false-无需写入
     *         true-need to write to memory false-no need
     */
    default boolean hasDataToWrite() {
        return true;
    }
}

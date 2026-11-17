package cn.itcraft.frogspawn.offheap;

import java.util.ArrayList;
import java.util.List;

/**
 * 非堆内存布局信息，记录类字段的内存布局
 * @author Helly Guo
 * <p>
 * Created on 2025-03-22 15:23
 */
public class OffHeapMemInfo {
    // 8字节对齐
    private static final int ALIGNMENT = 8;
    
    private final List<FieldType> fieldTypes = new ArrayList<>();
    // 字段大小（字节）
    private final List<Integer> fieldSizes = new ArrayList<>();
    // 字段偏移量（字节）
    private final List<Integer> fieldOffsets = new ArrayList<>();
    // 数组长度（仅数组字段有效）
    private final List<Integer> arrayLengths = new ArrayList<>();
    private int currentOffset = 0;
    private int totalSize = 0;
    
    /**
     * 添加字段到内存布局
     * @param fieldType 字段类型
     * @param size 字段大小（字节）
     */
    public void appendField(FieldType fieldType, int size) {
        appendField(fieldType, size, 0);
    }
    
    /**
     * 添加字段到内存布局
     * @param fieldType 字段类型
     * @param size 字段大小（字节）
     * @param arrayLength 数组长度（仅数组字段有效）
     */
    public void appendField(FieldType fieldType, int size, int arrayLength) {
        // 计算对齐后的偏移量
        int alignedOffset = alignUp(currentOffset, ALIGNMENT);

        fieldTypes.add(fieldType);
        fieldSizes.add(size);
        fieldOffsets.add(alignedOffset);
        arrayLengths.add(arrayLength);
        
        currentOffset = alignedOffset + size;
        totalSize = currentOffset;
    }
    
    /**
     * 获取字段数量
     */
    public int getFieldCount() {
        return fieldTypes.size();
    }
    
    /**
     * 获取字段类型
     * @param index 字段索引
     */
    public FieldType getFieldType(int index) {
        return fieldTypes.get(index);
    }
    
    /**
     * 获取字段大小（字节）
     * @param index 字段索引
     */
    public int getFieldSize(int index) {
        return fieldSizes.get(index);
    }
    
    /**
     * 获取字段偏移量（字节）
     * @param index 字段索引
     */
    public int getFieldOffset(int index) {
        return fieldOffsets.get(index);
    }
    
    /**
     * 获取数组长度
     * @param index 字段索引
     */
    public int getArrayLength(int index) {
        return arrayLengths.get(index);
    }
    
    /**
     * 获取对象内存总大小（字节，已对齐）
     */
    public int getObjectSize() {
        return alignUp(totalSize, ALIGNMENT);
    }
    
    /**
     * 获取数组数据总大小（字节）
     * 计算所有数组字段的数据大小总和
     */
    public long getTotalArrayDataSize() {
        long total = 0;
        for (int i = 0; i < fieldTypes.size(); i++) {
            FieldType type = fieldTypes.get(i);
            if (isArrayType(type)) {
                int elementSize = getElementSize(type);
                int length = arrayLengths.get(i);
                total += (long) elementSize * length;
            }
        }
        return total;
    }
    
    /**
     * 是否为数组类型
     */
    public boolean isArrayType(int index) {
        return isArrayType(fieldTypes.get(index));
    }
    
    /**
     * 是否为数组类型
     */
    private boolean isArrayType(FieldType type) {
        return type.name().startsWith("ARRAY_");
    }
    
    /**
     * 获取数组元素大小
     */
    private int getElementSize(FieldType arrayType) {
        switch (arrayType) {
            case ARRAY_BOOL: return 1;
            case ARRAY_BYTE: return 1;
            case ARRAY_CHAR: return 2;
            case ARRAY_SHORT: return 2;
            case ARRAY_INT: return 4;
            case ARRAY_LONG: return 8;
            case ARRAY_FLOAT: return 4;
            case ARRAY_DOUBLE: return 8;
            default: throw new IllegalArgumentException("Not an array type: " + arrayType);
        }
    }
    
    /**
     * 获取对应的基本类型
     */
    public FieldType getPrimitiveType(FieldType arrayType) {
        switch (arrayType) {
            case ARRAY_BOOL: return FieldType.BOOL;
            case ARRAY_BYTE: return FieldType.BYTE;
            case ARRAY_CHAR: return FieldType.CHAR;
            case ARRAY_SHORT: return FieldType.SHORT;
            case ARRAY_INT: return FieldType.INT;
            case ARRAY_LONG: return FieldType.LONG;
            case ARRAY_FLOAT: return FieldType.FLOAT;
            case ARRAY_DOUBLE: return FieldType.DOUBLE;
            default: throw new IllegalArgumentException("Not an array type: " + arrayType);
        }
    }
    
    /**
     * 对齐到指定边界
     * @param value 需要对齐的值
     * @param alignment 对齐边界（必须是2的幂）
     */
    private static int alignUp(int value, int alignment) {
        return (value + alignment - 1) & ~(alignment - 1);
    }
}

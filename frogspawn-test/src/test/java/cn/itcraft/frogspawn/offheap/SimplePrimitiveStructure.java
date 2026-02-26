package cn.itcraft.frogspawn.offheap;

import java.util.Arrays;

/**
 * @author Helly Guo
 * <p>
 * Created on 2026-02-26 09:02
 */
class SimplePrimitiveStructure implements PrimitiveStructure {
    private long address;
    private final long[] arrayAddresses = new long[0];
    private final int[] arrayLengths = new int[0];

    @Override
    public long getAddress() {
        return address;
    }

    @Override
    public void setAddress(long address) {
        this.address = address;
    }

    @Override
    public void reset() {
        address = 0;
        Arrays.fill(arrayAddresses, 0);
    }

    @Override
    public int getObjectSize() {
        return 8; // 只包含address字段
    }

    @Override
    public int getArrayFieldCount() {
        return 0; // 没有数组字段
    }

    @Override
    public long getArrayAddress(int index) {
        return arrayAddresses[index];
    }

    @Override
    public void setArrayAddress(int index, long address) {
        arrayAddresses[index] = address;
    }

    @Override
    public int getArrayLength(int index) {
        return arrayLengths[index];
    }

    @Override
    public void writeToMemory() {
        // 空实现，因为没有数据需要写入
    }

    @Override
    public void readFromMemory() {
        // 空实现，因为没有数据需要读取
    }

    @Override
    public boolean isInvalid() {
        return false;
    }

    @Override
    public int getMarkedId() {
        return -1;
    }

    @Override
    public void markId(int id) {
        // 空实现
    }
}

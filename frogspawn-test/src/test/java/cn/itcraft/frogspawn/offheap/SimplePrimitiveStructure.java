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
    public final long getAddress() {
        return address;
    }

    @Override
    public final void setAddress(long address) {
        this.address = address;
    }

    @Override
    public void reset() {
        address = 0;
        Arrays.fill(arrayAddresses, 0);
    }

    @Override
    public final int getObjectSize() {
        return 8;
    }

    @Override
    public final int getArrayFieldCount() {
        return 0;
    }

    @Override
    public final long getArrayAddress(int index) {
        return arrayAddresses[index];
    }

    @Override
    public final void setArrayAddress(int index, long address) {
        arrayAddresses[index] = address;
    }

    @Override
    public final int getArrayLength(int index) {
        return arrayLengths[index];
    }

    @Override
    public void writeToMemory() {
    }

    @Override
    public void readFromMemory() {
    }

    @Override
    public boolean hasDataToWrite() {
        return false;
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

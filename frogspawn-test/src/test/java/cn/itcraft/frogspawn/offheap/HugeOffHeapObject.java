package cn.itcraft.frogspawn.offheap;

import sun.misc.Unsafe;

/**
 * 大型复杂对象 - 原始非堆内存版本（PrimitiveStructure）
 * Huge complex object - Original off-heap version (PrimitiveStructure)
 *
 * @author Helly Guo
 * Created on 2026-03-24
 */
public class HugeOffHeapObject implements PrimitiveStructure {
    
    private static final Unsafe UNSAFE = UnsafeUtil.getTheUnsafe();
    
    private static final int LONG_ARRAY_SIZE = 100;
    private static final int INT_ARRAY_SIZE = 100;
    private static final int BYTE_ARRAY_SIZE = 256;
    
    private long address;
    private final long[] arrayAddresses = new long[3];
    private final int[] arrayLengths = {LONG_ARRAY_SIZE, INT_ARRAY_SIZE, BYTE_ARRAY_SIZE};
    
    private long field1;
    private long field2;
    private long field3;
    private long field4;
    private long field5;
    private long field6;
    private long field7;
    private long field8;
    private long field9;
    private long field10;
    
    private int field11;
    private int field12;
    private int field13;
    private int field14;
    private int field15;
    private int field16;
    private int field17;
    private int field18;
    private int field19;
    private int field20;
    
    private double field21;
    private double field22;
    private double field23;
    private double field24;
    private double field25;

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
        for (int i = 0; i < arrayAddresses.length; i++) {
            arrayAddresses[i] = 0;
        }
        field1 = field2 = field3 = field4 = field5 = 0;
        field6 = field7 = field8 = field9 = field10 = 0;
        field11 = field12 = field13 = field14 = field15 = 0;
        field16 = field17 = field18 = field19 = field20 = 0;
        field21 = field22 = field23 = field24 = field25 = 0.0;
    }

    @Override
    public final int getObjectSize() {
        return 160;
    }

    @Override
    public final int getArrayFieldCount() {
        return 3;
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
        if (address == 0) return;
        
        UNSAFE.putLong(address, field1);
        UNSAFE.putLong(address + 8, field2);
        UNSAFE.putLong(address + 16, field3);
        UNSAFE.putLong(address + 24, field4);
        UNSAFE.putLong(address + 32, field5);
        UNSAFE.putLong(address + 40, field6);
        UNSAFE.putLong(address + 48, field7);
        UNSAFE.putLong(address + 56, field8);
        UNSAFE.putLong(address + 64, field9);
        UNSAFE.putLong(address + 72, field10);
        
        UNSAFE.putInt(address + 80, field11);
        UNSAFE.putInt(address + 84, field12);
        UNSAFE.putInt(address + 88, field13);
        UNSAFE.putInt(address + 92, field14);
        UNSAFE.putInt(address + 96, field15);
        UNSAFE.putInt(address + 100, field16);
        UNSAFE.putInt(address + 104, field17);
        UNSAFE.putInt(address + 108, field18);
        UNSAFE.putInt(address + 112, field19);
        UNSAFE.putInt(address + 116, field20);
        
        UNSAFE.putDouble(address + 120, field21);
        UNSAFE.putDouble(address + 128, field22);
        UNSAFE.putDouble(address + 136, field23);
        UNSAFE.putDouble(address + 144, field24);
        UNSAFE.putDouble(address + 152, field25);
    }

    @Override
    public void readFromMemory() {
        if (address == 0) return;
        
        field1 = UNSAFE.getLong(address);
        field2 = UNSAFE.getLong(address + 8);
        field3 = UNSAFE.getLong(address + 16);
        field4 = UNSAFE.getLong(address + 24);
        field5 = UNSAFE.getLong(address + 32);
        field6 = UNSAFE.getLong(address + 40);
        field7 = UNSAFE.getLong(address + 48);
        field8 = UNSAFE.getLong(address + 56);
        field9 = UNSAFE.getLong(address + 64);
        field10 = UNSAFE.getLong(address + 72);
        
        field11 = UNSAFE.getInt(address + 80);
        field12 = UNSAFE.getInt(address + 84);
        field13 = UNSAFE.getInt(address + 88);
        field14 = UNSAFE.getInt(address + 92);
        field15 = UNSAFE.getInt(address + 96);
        field16 = UNSAFE.getInt(address + 100);
        field17 = UNSAFE.getInt(address + 104);
        field18 = UNSAFE.getInt(address + 108);
        field19 = UNSAFE.getInt(address + 112);
        field20 = UNSAFE.getInt(address + 116);
        
        field21 = UNSAFE.getDouble(address + 120);
        field22 = UNSAFE.getDouble(address + 128);
        field23 = UNSAFE.getDouble(address + 136);
        field24 = UNSAFE.getDouble(address + 144);
        field25 = UNSAFE.getDouble(address + 152);
    }

    @Override
    public boolean hasDataToWrite() {
        return true;
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
    }

    public void setData(long l1, long l2, int i1, int i2, double d1) {
        this.field1 = l1;
        this.field2 = l2;
        this.field11 = i1;
        this.field12 = i2;
        this.field21 = d1;
    }

    public long getField1() { return field1; }
    public long getField2() { return field2; }
    public int getField11() { return field11; }
    public int getField12() { return field12; }
    public double getField21() { return field21; }
}
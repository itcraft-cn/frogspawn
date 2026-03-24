package cn.itcraft.frogspawn.offheap;

import java.util.Arrays;

/**
 * 大型复杂对象 - 堆内存版本
 * Huge complex object - Heap version
 *
 * @author Helly Guo
 * Created on 2026-03-24
 */
public class HugeHeapObject implements cn.itcraft.frogspawn.Resettable {
    
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
    
    private final long[] longArray = new long[100];
    private final int[] intArray = new int[100];
    private final byte[] byteArray = new byte[256];

    @Override
    public void reset() {
        field1 = field2 = field3 = field4 = field5 = 0;
        field6 = field7 = field8 = field9 = field10 = 0;
        field11 = field12 = field13 = field14 = field15 = 0;
        field16 = field17 = field18 = field19 = field20 = 0;
        field21 = field22 = field23 = field24 = field25 = 0.0;
        Arrays.fill(longArray, 0);
        Arrays.fill(intArray, 0);
        Arrays.fill(byteArray, (byte) 0);
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
        longArray[0] = l1;
        longArray[1] = l2;
        intArray[0] = i1;
        intArray[1] = i2;
    }

    public long getField1() { return field1; }
    public long getField2() { return field2; }
    public int getField11() { return field11; }
    public int getField12() { return field12; }
    public double getField21() { return field21; }
}
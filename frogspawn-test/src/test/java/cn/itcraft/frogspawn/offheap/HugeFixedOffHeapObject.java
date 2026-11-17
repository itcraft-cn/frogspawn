package cn.itcraft.frogspawn.offheap;

import sun.misc.Unsafe;

/**
 * 大型复杂对象 - 固定非堆内存版本
 * Huge complex object - Fixed off-heap version
 *
 * 内存布局：
 * - 10 long (80 bytes)
 * - 10 int (40 bytes)
 * - 5 double (40 bytes)
 * - 1 long[100] (800 bytes)
 * - 1 int[100] (400 bytes)
 * - 1 byte[256] (256 bytes)
 * Total: 1616 bytes, 对齐到 2048 bytes
 *
 * @author Helly Guo
 * Created on 2026-03-24
 */
public class HugeFixedOffHeapObject implements FixedOffHeapObject {
    
    private static final Unsafe UNSAFE = UnsafeUtil.getTheUnsafe();
    public static final int OBJECT_SIZE = 2048;
    
    private static final int LONG_ARRAY_OFFSET = 160;
    private static final int INT_ARRAY_OFFSET = 960;
    private static final int BYTE_ARRAY_OFFSET = 1360;
    
    private int index;
    private long address;

    @Override
    public final int index() {
        return index;
    }

    @Override
    public final void index(int idx) {
        this.index = idx;
    }

    @Override
    public final long address() {
        return address;
    }

    @Override
    public final void address(long addr) {
        this.address = addr;
    }

    @Override
    public void reset() {
        if (address != 0) {
            UNSAFE.setMemory(address, OBJECT_SIZE, (byte) 0);
        }
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
        if (address == 0) {
            return;
        }
        
        UNSAFE.putLong(address, l1);
        UNSAFE.putLong(address + 8, l2);
        UNSAFE.putInt(address + 80, i1);
        UNSAFE.putInt(address + 84, i2);
        UNSAFE.putDouble(address + 120, d1);
        
        UNSAFE.putLong(address + LONG_ARRAY_OFFSET, l1);
        UNSAFE.putLong(address + LONG_ARRAY_OFFSET + 8, l2);
        UNSAFE.putInt(address + INT_ARRAY_OFFSET, i1);
        UNSAFE.putInt(address + INT_ARRAY_OFFSET + 4, i2);
    }

    public long getField1() {
        return address != 0 ? UNSAFE.getLong(address) : 0;
    }

    public long getField2() {
        return address != 0 ? UNSAFE.getLong(address + 8) : 0;
    }

    public int getField11() {
        return address != 0 ? UNSAFE.getInt(address + 80) : 0;
    }

    public int getField12() {
        return address != 0 ? UNSAFE.getInt(address + 84) : 0;
    }

    public double getField21() {
        return address != 0 ? UNSAFE.getDouble(address + 120) : 0.0;
    }
}
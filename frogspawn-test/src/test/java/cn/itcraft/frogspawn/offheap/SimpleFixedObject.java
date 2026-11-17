package cn.itcraft.frogspawn.offheap;

import cn.itcraft.frogspawn.platform.MemoryAccess;
import cn.itcraft.frogspawn.platform.PlatformMemory;

/**
 * 简单固定非堆对象实现
 * Simple fixed off-heap object implementation
 *
 * @author Helly Guo
 * Created on 2026-03-24
 */
class SimpleFixedObject implements FixedOffHeapObject {
    
    private static final MemoryAccess MEMORY = PlatformMemory.ACCESS;
    
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
    
    // 业务方法示例
    public void setValue(long value) {
        MEMORY.putLong(address, value);
    }
    
    public long getValue() {
        return MEMORY.getLong(address);
    }
}
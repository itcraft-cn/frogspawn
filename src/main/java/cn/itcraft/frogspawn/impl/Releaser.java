package cn.itcraft.frogspawn.impl;

/**
 * @author Helly Guo
 * <p>
 * Created on 2025-03-18 07:40
 */
interface Releaser<T> {
    void release(T used);
}

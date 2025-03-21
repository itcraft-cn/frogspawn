package cn.itcraft.frogspawn.offheap;

import cn.itcraft.frogspawn.Resettable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

/**
 * @author Helly Guo
 * <p>
 * Created on 2025-03-21 16:39
 */
public class ClassCheckerTest {

    private static final TestTarget[] TARGETS = new TestTarget[]{
            new TestTarget(Demo1.class, true),
            new TestTarget(Demo2.class, true),
            new TestTarget(Demo3.class, false),
            new TestTarget(Demo4.class, false),
            new TestTarget(Demo5.class, false),
            };

    @Test
    public void testCheck() {
        for (TestTarget target : TARGETS) {
            Executable executable = () -> test(target.getTarget());
            if (target.isShouldThrowEx()) {
                Assertions.assertThrows(Exception.class, executable);
            } else {
                Assertions.assertDoesNotThrow(executable, "test");
            }
        }
    }

    private void test(Class<?> clazz) {
        ClassChecker.checkClassValid(clazz, PrimitiveStructure.class);
    }

    private static final class Demo1 implements Resettable {

        private Integer a1;
        private Long a2;
        private Double a3;
        private String[] a4;

        @Override
        public void reset() {
        }

        @Override
        public int getMarkedId() {
            return 0;
        }

        @Override
        public void markId(int id) {
        }
    }

    private static class TestTarget {

        private final Class<?> target;
        private final boolean shouldThrowEx;

        public TestTarget(Class<?> target, boolean shouldThrowEx) {
            this.target = target;
            this.shouldThrowEx = shouldThrowEx;
        }

        public Class<?> getTarget() {
            return target;
        }

        public boolean isShouldThrowEx() {
            return shouldThrowEx;
        }
    }

    private static final class Demo2 implements PrimitiveStructure {

        private Integer a1;
        private Long a2;
        private Double a3;
        private String[] a4;

        @Override
        public void reset() {
        }

        @Override
        public int getMarkedId() {
            return 0;
        }

        @Override
        public void markId(int id) {
        }
    }

    private static final class Demo3 implements PrimitiveStructure {

        private int a1;
        private long a2;
        private double a3;
        private int[] a4;

        @Override
        public void reset() {
        }

        @Override
        public int getMarkedId() {
            return 0;
        }

        @Override
        public void markId(int id) {
        }
    }

    private final class Demo4 implements PrimitiveStructure {

        private int a1;
        private long a2;
        private double a3;
        private int[] a4;

        @Override
        public void reset() {
        }

        @Override
        public int getMarkedId() {
            return 0;
        }

        @Override
        public void markId(int id) {
        }
    }

    private final class Demo5 implements PrimitiveStructure {

        private int a1;
        private long a2;
        private double a3;
        private int[] a4;

        private int this$0;

        @Override
        public void reset() {
        }

        @Override
        public int getMarkedId() {
            return 0;
        }

        @Override
        public void markId(int id) {
        }
    }


}

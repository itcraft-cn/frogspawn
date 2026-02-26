package cn.itcraft.frogspawn.offheap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * @author Helly Guo
 * <p>
 * Created on 2025-03-21 16:23
 */
final class ClassUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClassUtil.class);

    private static final Pattern THIS_VAR = Pattern.compile("^this\\$\\d+\\$?$");

    private ClassUtil() {
    }

    public static void checkClassValid(Class<?> clazz, Class<?> expected) {
        String expectedName = expected.getName();
        if (Arrays.stream(clazz.getGenericInterfaces())
                  .noneMatch(t -> expectedName.equals(t.getTypeName()))) {
            LOGGER.warn("{} is not implemented {}", clazz, expected);
            throw new RuntimeException("not supported");
        }
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (THIS_VAR.matcher(field.getName()).matches()) {
                continue;
            }
            field.setAccessible(true);
            checkFieldValid(field);
        }
    }

    private static void checkFieldValid(Field field) {
        Class<?> fieldType = field.getType();
        if (fieldType.isPrimitive()) {
            LOGGER.debug("{} is primitive type, {}", field, fieldType);
        } else if (fieldType.isArray()) {
            Class<?> elementType = fieldType.getComponentType();
            if (elementType.isPrimitive()) {
                LOGGER.debug("{} is primitive array, {}", field, elementType);
            } else {
                LOGGER.warn("{} is array, but not support: {}", field, elementType);
                throw new RuntimeException("not supported");
            }
        } else {
            LOGGER.warn("{} is not support: {}", field, fieldType);
            throw new RuntimeException("not supported");
        }
    }

    public static void appendField(OffHeapMemInfo info, Field field) {
        Class<?> fieldType = field.getType();
        if (boolean.class.equals(fieldType)) {
            info.appendField(FieldType.BOOL, 1);
        } else if (byte.class.equals(fieldType)) {
            info.appendField(FieldType.BYTE, 1);
        } else if (char.class.equals(fieldType)) {
            info.appendField(FieldType.CHAR, 2);
        } else if (short.class.equals(fieldType)) {
            info.appendField(FieldType.SHORT, 2);
        } else if (int.class.equals(fieldType)) {
            info.appendField(FieldType.INT, 4);
        } else if (long.class.equals(fieldType)) {
            info.appendField(FieldType.LONG, 8);
        } else if (float.class.equals(fieldType)) {
            info.appendField(FieldType.FLOAT, 4);
        } else if (double.class.equals(fieldType)) {
            info.appendField(FieldType.DOUBLE, 8);
        } else if (fieldType.isArray()) {
            Class<?> componentType = fieldType.getComponentType();
            FieldType arrayFieldType = getArrayFieldType(componentType);
            // 数组字段在对象内存中存储指针（8字节）
            // 数组长度默认为0，将在运行时确定
            info.appendField(arrayFieldType, 8, 0);
        }
    }

    private static FieldType getArrayFieldType(Class<?> componentType) {
        if (boolean.class.equals(componentType)) {
            return FieldType.ARRAY_BOOL;
        } else if (byte.class.equals(componentType)) {
            return FieldType.ARRAY_BYTE;
        } else if (char.class.equals(componentType)) {
            return FieldType.ARRAY_CHAR;
        } else if (short.class.equals(componentType)) {
            return FieldType.ARRAY_SHORT;
        } else if (int.class.equals(componentType)) {
            return FieldType.ARRAY_INT;
        } else if (long.class.equals(componentType)) {
            return FieldType.ARRAY_LONG;
        } else if (float.class.equals(componentType)) {
            return FieldType.ARRAY_FLOAT;
        } else if (double.class.equals(componentType)) {
            return FieldType.ARRAY_DOUBLE;
        } else {
            throw new IllegalArgumentException("Unsupported array component type: " + componentType);
        }
    }

    /**
     * 分析类的内存布局
     *
     * @param clazz 要分析的类（必须实现PrimitiveStructure接口）
     * @return 内存布局信息
     */
    public static OffHeapMemInfo analyzeClassLayout(Class<?> clazz) {
        checkClassValid(clazz, PrimitiveStructure.class);

        OffHeapMemInfo info = new OffHeapMemInfo();
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            if (THIS_VAR.matcher(field.getName()).matches()) {
                // 跳过内部类持有的外部类引用字段
                continue;
            }
            field.setAccessible(true);
            appendField(info, field);
        }

        return info;
    }
}

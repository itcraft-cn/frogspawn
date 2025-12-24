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
            info.appendField(FieldType.BYTE, Byte.SIZE);
        } else if (char.class.equals(fieldType)) {
            info.appendField(FieldType.CHAR, Character.SIZE);
        } else if (short.class.equals(fieldType)) {
            info.appendField(FieldType.SHORT, Short.SIZE);
        } else if (int.class.equals(fieldType)) {
            info.appendField(FieldType.INT, Integer.SIZE);
        } else if (long.class.equals(fieldType)) {
            info.appendField(FieldType.LONG, Long.SIZE);
        } else if (float.class.equals(fieldType)) {
            info.appendField(FieldType.FLOAT, Float.SIZE);
        } else if (double.class.equals(fieldType)) {
            info.appendField(FieldType.DOUBLE, Double.SIZE);
        }
    }
}
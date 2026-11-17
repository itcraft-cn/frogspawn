package cn.itcraft.frogspawn.offheap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * @author Helly Guo
 * <p>
 * Created on 2025-03-21 16:23
 */
final class ClassChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClassChecker.class);

    private static final Pattern THIS_VAR = Pattern.compile("^this\\$\\d+\\$?$");

    private ClassChecker() {
    }

    public static void checkClassValid(Class<?> clazz, Class<?> expected) {
        String expectedName = expected.getName();
        if (Arrays.stream(clazz.getGenericInterfaces())
                  .noneMatch(t -> expectedName.equals(t.getTypeName()))) {
            LOGGER.warn("{} is not implemented {}", clazz, expected);
            throw new UnsupportedOperationException("not supported");
        }
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (THIS_VAR.matcher(field.getName()).matches()) {
                continue;
            }
            makeAccessible(field);
            checkFieldValid(field);
        }
    }

    private static void makeAccessible(Field field) {
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            field.setAccessible(true);
            return null;
        });
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
                throw new UnsupportedOperationException("not supported");
            }
        } else {
            LOGGER.warn("{} is not support: {}", field, fieldType);
            throw new UnsupportedOperationException("not supported");
        }
    }
}

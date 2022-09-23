package com.xeasy.fingerprintpress.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ReflexUtil {


    public static Object getField4Obj(Object object, String fieldName) {
        try {
            Field declaredField = object.getClass().getDeclaredField(fieldName);
            declaredField.setAccessible(true);
            return declaredField.get(object);
        } catch (Exception e) {
            e.printStackTrace();
            return e;
        }
    }

    public static void setField4Obj(String fieldName, Object object, Object value) {
        try {
            Field declaredField = object.getClass().getDeclaredField(fieldName);
            declaredField.setAccessible(true);
            declaredField.set(object, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Object runMethod(Object object, String methodName, Object[] params, Class<?>... paramTypes) {
        return runMethod(object.getClass(), object, methodName, params, paramTypes);
    }

    public static Object runMethod(Class<?> clazz, Object object, String methodName, Object[] params, Class<?>... paramTypes) {
        try {
            if (null == params || params.length == 0) {
                Method declaredMethod = clazz.getDeclaredMethod(methodName);
                declaredMethod.setAccessible(true);
                return declaredMethod.invoke(object);
            }
            Method method = clazz.getDeclaredMethod(methodName, paramTypes);
            method.setAccessible(true);
            return method.invoke(object, params);
        } catch (Exception e) {
            return e;
        }
    }

    public static Object runStaticMethod(Class<?> clazz, String methodName, Object[] params, Class<?>... paramTypes) {


        try {
            if (null == params || params.length == 0) {
                Method declaredMethod = clazz.getDeclaredMethod(methodName);
                declaredMethod.setAccessible(true);
                return declaredMethod.invoke(null);
            }

            Method method = clazz.getDeclaredMethod(methodName, paramTypes);
            method.setAccessible(true);
            return method.invoke(null, params);
        } catch (Exception e) {
            return e;
        }
    }

}

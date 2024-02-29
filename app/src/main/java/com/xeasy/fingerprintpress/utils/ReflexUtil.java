package com.xeasy.fingerprintpress.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import de.robv.android.xposed.XposedBridge;

public class ReflexUtil {

//    static {
//        // 测试ndk 待删除
//        System.loadLibrary("native-jni");
//    }



    public static Method findMethodIfParamExistWithLog(Class<?> clazz, String methodName, Class<?> ... params ) {
        Class<?> clazzTemp = clazz;
        while (clazzTemp != null ) {
            loopOut:
            for (Method declaredMethod : clazzTemp.getDeclaredMethods()) {
                XposedBridge.log(" 类名 = " + clazzTemp.getName() +  " 方法名=== " + declaredMethod.getName());
                if ( methodName.equals(declaredMethod.getName()) ) {
                    declaredMethod.setAccessible(true);
                    // 如果要查的有参数 比较参数是否存在 不考虑前后顺序
                    if ( null != params && params.length > 0 ) {
                        Class<?>[] parameterTypes = declaredMethod.getParameterTypes();
                        if ( parameterTypes.length == 0 ) {
                            // 该方法没有参数, 进入下个循环
                            continue;
                        }
                        List<Class<?>> classes = Arrays.asList(parameterTypes);
                        for (Class<?> arg : params) {
                            if (! classes.contains(arg) ) {
                                continue loopOut;
                            }
                        }
                        return declaredMethod;
                    } else {
                        return declaredMethod;
                    }
                }
            }
            clazzTemp = clazzTemp.getSuperclass();
        }
        return null;
    }

    public static Method findMethodIfParamExist(Class<?> clazz, String methodName, Class<?> ... params ) {
        Class<?> clazzTemp = clazz;
        while (clazzTemp != null ) {
            loopOut:
            for (Method declaredMethod : clazzTemp.getDeclaredMethods()) {
//                XposedBridge.log(" 类名 = " + clazzTemp.getName() +  " 方法名=== " + declaredMethod.getName());
                if ( methodName.equals(declaredMethod.getName()) ) {
                    declaredMethod.setAccessible(true);
                    // 如果要查的有参数 比较参数是否存在 不考虑前后顺序
                    if ( null != params && params.length > 0 ) {
                        Class<?>[] parameterTypes = declaredMethod.getParameterTypes();
                        if ( parameterTypes.length == 0 ) {
                            // 该方法没有参数, 进入下个循环
                            continue;
                        }
                        List<Class<?>> classes = Arrays.asList(parameterTypes);
                        for (Class<?> arg : params) {
                            if (! classes.contains(arg) ) {
                                continue loopOut;
                            }
                        }
                        return declaredMethod;
                    } else {
                        return declaredMethod;
                    }
                }
            }
            clazzTemp = clazzTemp.getSuperclass();
        }
        return null;
    }

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

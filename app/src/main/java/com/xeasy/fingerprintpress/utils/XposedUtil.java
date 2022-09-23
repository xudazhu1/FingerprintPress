package com.xeasy.fingerprintpress.utils;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class XposedUtil {

    public static Class<?> findClass4Xposed(String classPath, ClassLoader classLoader) {
        try {
            return XposedHelpers.findClass(classPath, classLoader);
        } catch (Exception e) {
            return Exception.class;
        }
    }

}

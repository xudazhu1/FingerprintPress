package com.xeasy.fingerprintpress.hook;

public class HookConstant {
    /**
     * 设备是否醒着 用于指纹判断
     */
    public static boolean awake = true;
    /**
     * 最后一次指纹验证成功还是失败
     */
    public static boolean authenticationSucceeded = false;
    /**
     * 最后一次指纹验证成功 的userid
     */
    public static int userid = 0;
    /**
     * 最后一次指纹验证成功 的 指纹传感器id
     */
    public static int sensorIdS = 0;
    /**
     * 最后一次指纹验证成功 的时间戳
     */
    public static Long times = System.currentTimeMillis();
    /**
     * 指纹认证成功和按压电源键的连贯间隔要求
     */
    public static final int TIMEOUT = 2200;
}

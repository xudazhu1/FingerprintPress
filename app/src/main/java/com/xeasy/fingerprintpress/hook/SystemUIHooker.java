package com.xeasy.fingerprintpress.hook;


import static com.topjohnwu.superuser.internal.UiThreadHandler.handler;


import android.annotation.SuppressLint;
import android.app.AndroidAppHelper;
import android.app.Application;
import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.media.AudioAttributes;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.Process;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;

import com.xeasy.fingerprintpress.utils.ReflexUtil;
import com.xeasy.fingerprintpress.utils.XposedUtil;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

@SuppressWarnings("deprecation")
public class SystemUIHooker implements IXposedHookLoadPackage {

    private static final String LOG_PREV = "Fingerprintpress---";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) {

        // hook systemui 指纹解锁逻辑
        if (loadPackageParam.packageName.equals("com.android.systemui")) {
            try {
                hookFingerprintByConstructor(loadPackageParam.classLoader);
            } catch (Exception e) {
                XposedBridge.log(LOG_PREV + "hook -- hookFingerprintByConstructor 错误");
                XposedBridge.log(e);
            }

        }
        //  如果是安卓13及以下 hook系统核心 修改指纹震动逻辑 查看安卓兼容性 完成
        if (loadPackageParam.packageName.equals("android") && android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.TIRAMISU) {
            try {
                vibrateSuccessAndError(loadPackageParam.classLoader);
            } catch (Exception e) {
                XposedBridge.log(LOG_PREV + "hook -- vibrateSuccessAndError 错误");
                XposedBridge.log(e);
            }
        }
    }


    // hook指纹验证的震动 10 / 11 / 12 / 13 适配完成
    // com.android.server.biometrics.sensors.AcquisitionClient#vibrateSuccess
    private void vibrateSuccessAndError(ClassLoader classLoader) {
        // 安卓 12
        Class<?> clazz = XposedUtil.findClass4Xposed(
                "com.android.server.biometrics.sensors.AcquisitionClient", classLoader);
        if (clazz == Exception.class) {
            // 安卓10 11
            clazz = XposedUtil.findClass4Xposed(
                    "com.android.server.biometrics.ClientMonitor", classLoader);
        }

        if (clazz != Exception.class) {
            //Hook有参构造函数，修改参数 pass real auth token once fp HAL supports it
            XposedHelpers.findAndHookMethod(clazz, "vibrateSuccess"
                    , new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            // 如果是指纹识别 并且 是息屏的 阻止指纹的震动方法(此方法) 12 FingerprintAuthenticationClient 10/11 FingerprintAuthClient
                            if (param.thisObject.getClass().getSimpleName().contains("FingerprintAuth")) {
                                //  FingerprintAuthenticationClient . isPointerDown 表示是按压指纹
                                Application application = AndroidAppHelper.currentApplication();
                                PowerManager pm = (PowerManager) application.getSystemService(Context.POWER_SERVICE);
                                boolean isScreenOn = pm.isInteractive();//如果为true，则表示屏幕“亮”了，否则屏幕“暗”了。
                                if (!isScreenOn) {
                                    param.setResult(null);
//                                    XposedBridge.log(LOG_PREV + "拦截了指纹成功震动");
                                }
                            }
                        }
                    });


            // 失败
            XposedHelpers.findAndHookMethod(clazz, "vibrateError"
                    , new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            // 如果是指纹识别 并且 是息屏的 阻止指纹的震动方法(此方法) 12 FingerprintAuthenticationClient 10/11 FingerprintAuthClient
                            if (param.thisObject.getClass().getSimpleName().contains("FingerprintAuth")) {
                                //  FingerprintAuthenticationClient . isPointerDown 表示是按压指纹
                                Application application = AndroidAppHelper.currentApplication();
                                PowerManager pm = (PowerManager) application.getSystemService(Context.POWER_SERVICE);
                                boolean isScreenOn = pm.isInteractive();//如果为true，则表示屏幕“亮”了，否则屏幕“暗”了。
                                if (!isScreenOn) {
                                    param.setResult(null);
//                                    XposedBridge.log(LOG_PREV + "拦截了指纹失败震动");
                                }
                            }
                        }
                    });
//            XposedBridge.log(LOG_PREV + " vibrateSuccess vibrateError hook 完成");
        }

    }


    // todo 测试hook指纹解锁 等待检查 安卓 10 - 13 兼容性
    // com.android.keyguard.KeyguardUpdateMonitor#mFingerprintAuthenticationCallback
    private void hookFingerprintByConstructor(ClassLoader classLoader) {

        if (android.os.Build.VERSION.SDK_INT == 34) {
            // 安卓14 指纹的震动 com.android.systemui.statusbar.VibratorHelper # vibrate(int, String, VibrationEffect, String, VibrationAttributes)
            XposedHelpers.findAndHookMethod(
                    XposedHelpers.findClass("com.android.systemui.statusbar.VibratorHelper", classLoader)
                    , "vibrate"
                    , int.class, String.class, VibrationEffect.class, String.class, VibrationAttributes.class
                    , new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            // 如果是指纹识别 并且 是息屏的 阻止指纹的震动方法(此方法) 12 FingerprintAuthenticationClient 10/11 FingerprintAuthClient
//                        XposedBridge.log("生物认证失败, = " + param.args[3]);
                            String reason = (String) param.args[3];
                            // 指纹 且 黑屏 的话, 阻止执行
                            if (null != reason && reason.contains("FINGERPRINT")) {
                                //  FingerprintAuthenticationClient . isPointerDown 表示是按压指纹
                                Application application = AndroidAppHelper.currentApplication();
                                PowerManager pm = (PowerManager) application.getSystemService(Context.POWER_SERVICE);
                                boolean isScreenOn = pm.isInteractive();//如果为true，则表示屏幕“亮”了，否则屏幕“暗”了。
                                if (!isScreenOn) {
                                    param.setResult(null);
//                                    XposedBridge.log(LOG_PREV + "拦截了指纹失败震动");
                                }
                            }
                        }
                    });
        }


        // 此类 安卓 10 - 13 兼容
        Class<?> aClass = XposedHelpers.findClass("com.android.keyguard.KeyguardUpdateMonitor", classLoader);

        // 测试 startListeningForFingerprint 方法的参数 android.hardware.biometrics.BiometricSourceType.FINGERPRINT
        /*Method isUnlockingWithFingerprintAllowed = ReflexUtil.findMethodIfParamExistWithLog(aClass, "isUnlockingWithFingerprintAllowed");
        if (null == isUnlockingWithFingerprintAllowed) {
            XposedBridge.log("方法 isUnlockingWithFingerprintAllowed 未找到");
        } else {
            Class<?>[] parameterTypes = isUnlockingWithFingerprintAllowed.getParameterTypes();
            XposedBridge.log("isUnlockingWithFingerprintAllowed 长度 = " + parameterTypes.length);
            for (Class<?> parameterType : parameterTypes) {
                XposedBridge.log("参数 == " + parameterType);
            }
        }*/
        /*XposedBridge.hookMethod(handleFingerprintLockoutReset, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if ( param.args.length > 0 ) {
                    XposedBridge.log("参数为=== " + param.args[0]);
                }

            }
        });*/


        XposedBridge.hookAllConstructors(
                aClass,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        // 获取  mFingerprintAuthenticationCallback  此字段 安卓 10 - 13 兼容
                        Object mFingerprintAuthenticationCallback = ReflexUtil.getField4Obj(param.thisObject, "mFingerprintAuthenticationCallback");
                        Class<?> aClass1 = mFingerprintAuthenticationCallback.getClass();
//                        XposedBridge.log(LOG_PREV + "查看内部类的class = :  " + aClass1);
                        // hook指纹成功事件 和指纹失败事件
                        onAuthenticationSucceeded(aClass1, classLoader);

                        // 安卓14 com.android.keyguard.KeyguardUpdateMonitor # dispatchStartedWakingUp
                        if (android.os.Build.VERSION.SDK_INT == 34) {
                            // 获取 mHandler 看看
                            Object mHandler = ReflexUtil.getField4Obj(param.thisObject, "mHandler");
                            Object outObject = param.thisObject;
                            Object outParam = param;
//                            XposedBridge.log("反射到 mHandler == " + mHandler);
                            XposedHelpers.findAndHookMethod(mHandler.getClass(), "handleMessage", Message.class
                                    , new XC_MethodHook() {
                                        @Override
                                        protected void beforeHookedMethod(MethodHookParam param) {
                                            Message message = (Message) param.args[0];
                                            // private  final int MSG_STARTED_WAKING_UP = 319;
                                            // private static final int MSG_STARTED_GOING_TO_SLEEP = 321;
//                                            XposedBridge.log("收到 Message == " + message.what);
                                            if (321 == message.what) {
                                                // 开始睡眠, 将awake改为false
                                                HookConstant.awake = false;
                                            }
                                            if (319 == message.what) {
                                                // 唤醒 了
//                                                XposedBridge.log(LOG_PREV + "触发唤醒事件拉!");
                                                HookConstant.awake = true;
                                                // 时间差验证
                                                if (HookConstant.authenticationSucceeded && (System.currentTimeMillis() - HookConstant.times) < HookConstant.TIMEOUT) {
                                                    param.setResult(null);
                                                    // todo 解锁设备的震动 待测试安卓版本兼容性
                                                    vibrate4Unlock();
                                                    HookConstant.authenticationSucceeded = false;
                                                    // 尝试解锁设备 √
                                                    unlock((MethodHookParam) outParam);
                                                    XposedBridge.log(LOG_PREV + "尝试解锁设备!");

                                                    // 停止指纹监听 延迟执行有效 √
                                                    handler.postDelayed(() -> {
                                                        // 停止监听指纹 此方法兼容 10-13
                                                        Object startWakeAndUnlock = ReflexUtil.runMethod(outObject,
                                                                "stopListeningForFingerprint", null
                                                        );
                                                        if (startWakeAndUnlock instanceof Exception) {
                                                            XposedBridge.log((Exception) startWakeAndUnlock);
                                                        }
                                                        // 停止监听指纹 此方法兼容 10-13
                                                        Object startWakeAndUnlock2 = ReflexUtil.runMethod(outObject,
                                                                "setFingerprintRunningState", new Object[]{0}, int.class
                                                        );
                                                        if (startWakeAndUnlock2 instanceof Exception) {
                                                            XposedBridge.log((Exception) startWakeAndUnlock2);
                                                        }
                                                    }, 250);
                                                }
                                            }
                                        }
                                    });
                        } else {
                            // hook 唤醒 和 解锁 的逻辑
                            handleStartedWakingUpAndSleep(classLoader);
                        }


                    }
                });
        Method handleFingerprintError = ReflexUtil.findMethodIfParamExist(aClass, "handleFingerprintError", int.class, String.class);
        if (null != handleFingerprintError) {
            XposedBridge.hookMethod(handleFingerprintError
                    , new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            // 获取  错误id 和 错误原因
                            int errId = (int) param.args[0];
                            if (errId == 7 || errId == 9) {
                                Locale locale = AndroidAppHelper.currentApplication().getResources().getConfiguration().getLocales().get(0);
                                if (locale.getLanguage().equals(new Locale("zh").getLanguage())) {
                                    param.args[1] = "尝试次数过多, 不用等待也可重试😄";
                                }
                            }

                        }
                    });
        } else {
            // todo 安卓14 未适配
            XposedBridge.log("未找到 handleFingerprintError , 指纹验证失败时的提示修改失败");
        }

    }

    // android.hardware.fingerprint.FingerprintManager#resetLockout
    private void resetLockout(ClassLoader classLoader) {
        final Class<?> clazz = XposedHelpers.findClass(
                "android.hardware.fingerprint.FingerprintManager", classLoader);
        //Hook有参构造函数，修改参数 pass real auth token once fp HAL supports it
        XposedHelpers.findAndHookMethod(clazz, "resetLockout",
                int.class, int.class, byte[].class
                , new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        XposedBridge.log(LOG_PREV + "参数 resetLockout hook= " + Arrays.toString(param.args));
                    }
                });
        XposedBridge.log(LOG_PREV + " FingerprintManager resetLockout hook 完成");
    }

    // com.android.keyguard.KeyguardUpdateMonitor#handleStartedWakingUp
    private void handleStartedWakingUpAndSleep(ClassLoader classLoader) {
        final Class<?> clazz = XposedHelpers.findClass(
                "com.android.keyguard.KeyguardUpdateMonitor", classLoader);

        XposedHelpers.findAndHookMethod(clazz, "handleStartedWakingUp"
                , new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
//                        XposedBridge.log(LOG_PREV + "触发唤醒事件拉!");
                        HookConstant.awake = true;
//                        ReflexUtil.runMethod(param.thisObject, "updateBiometricListeningState", 0);
                        // 时间差验证
                        if (HookConstant.authenticationSucceeded && (System.currentTimeMillis() - HookConstant.times) < HookConstant.TIMEOUT) {
                            param.setResult(null);
                            // todo 解锁设备的震动 待测试安卓版本兼容性
                            vibrate4Unlock();
//                            var1.vibrate(var2, var3, var4, var5.toString(), build);
                            HookConstant.authenticationSucceeded = false;
                            // 尝试解锁设备 √
                            unlock(param);
//                            XposedBridge.log(LOG_PREV + "尝试解锁设备!");

                            // 停止指纹监听 延迟执行有效 √
                            handler.postDelayed(() -> {
                                // 停止监听指纹 此方法兼容 10-13
                                Object startWakeAndUnlock = ReflexUtil.runMethod(param.thisObject,
                                        "stopListeningForFingerprint", null
                                );
                                if (startWakeAndUnlock instanceof Exception) {
                                    XposedBridge.log((Exception) startWakeAndUnlock);
                                }
                                // 停止监听指纹 此方法兼容 10-13
                                Object startWakeAndUnlock2 = ReflexUtil.runMethod(param.thisObject,
                                        "setFingerprintRunningState", new Object[]{0}, int.class
                                );
                                if (startWakeAndUnlock2 instanceof Exception) {
                                    XposedBridge.log((Exception) startWakeAndUnlock2);
                                }
                            }, 250);
                        }
                    }
                });
        //Hook有参构造函数，修改参数 此方法兼容 10-13
        XposedHelpers.findAndHookMethod(clazz, "handleStartedGoingToSleep"
                , int.class
                , new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        // 开始睡眠, 将awake改为false
                        HookConstant.awake = false;
                    }
                });
        XposedBridge.log(LOG_PREV + " hook 完成 -- handleStartedWakingUpAndSleep ");
    }

    // 解锁设备
    private void unlock(XC_MethodHook.MethodHookParam param) {
        // 安卓14
        if (android.os.Build.VERSION.SDK_INT == 34) {
//            onFingerprintAuthenticated(userId, isStrongBiometric);
//            setFingerprintRunningState(BIOMETRIC_STATE_STOPPED);
            // 运行解锁方法
            ReflexUtil.runMethod(param.thisObject, "onFingerprintAuthenticated",
                    new Object[]{HookConstant.userid, true}, int.class, boolean.class
            );
            // 停止指纹监听
            ReflexUtil.runMethod(param.thisObject, "setFingerprintRunningState",
                    new Object[]{0}, int.class
            );
            return;
        }

        // Android 11 / 12 / 13
        Object handleFingerprintAuthenticated = ReflexUtil.runMethod(param.thisObject, "handleFingerprintAuthenticated",
                new Object[]{HookConstant.userid, true}, int.class, boolean.class
        );
        // Android 10
        if (handleFingerprintAuthenticated instanceof Exception) {
            ReflexUtil.runMethod(param.thisObject, "handleFingerprintAuthenticated",
                    new Object[]{HookConstant.userid}, int.class
            );
        }
    }

    @SuppressLint("MissingPermission")
    private void vibrate4Unlock() {
        try {
            Vibrator vibrator = AndroidAppHelper.currentApplication().getSystemService(Vibrator.class);
            // 震动
            int var2 = Process.myUid();
            String opPkg = AndroidAppHelper.currentApplication().getOpPackageName();
            VibrationEffect vibrationEffect = (VibrationEffect) ReflexUtil.runStaticMethod(VibrationEffect.class, "get", new Object[]{0}, int.class);
            String var5 = this.getClass().getSimpleName() +
                    "::success";
            AudioAttributes audioAttributes = (new AudioAttributes.Builder())
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).build();
//            Object vibrate = ReflexUtil.runMethod(Vibrator.class, vibrator, "vibrate", new Object[]{var2, opPkg, vibrationEffect, var5, audioAttributes}
//                    , int.class, String.class, VibrationEffect.class, String.class, AudioAttributes.class
//            );
            // 放弃反射 手动发送震动
            vibrator.vibrate(vibrationEffect, audioAttributes);
//            XposedBridge.log(LOG_PREV + "发送解锁震动!!");
//            if ( vibrate instanceof Exception) {
//                XposedBridge.log(LOG_PREV + "发送解锁震动失败!!");
//                XposedBridge.log((Exception)vibrate);
//            } else {
//                XposedBridge.log(LOG_PREV + "发送解锁震动成功!! =" + vibrate);
//            }
        } catch (Exception e) {
            XposedBridge.log(LOG_PREV + "发送解锁震动失败!!");
            XposedBridge.log(e);
        }
    }

    /**
     * hook目标 com.android.keyguard.KeyguardUpdateMonitor.mFingerprintAuthenticationCallback#onAuthenticationSucceeded
     * hook指纹成功事件 和指纹失败事件
     * todo 等待 安卓 10 - 13 兼容
     *
     * @param clazz       内部类的class 是动态获取的
     * @param classLoader c
     */
    private void onAuthenticationSucceeded(Class<?> clazz, ClassLoader classLoader) {
        final Class<?> args0 = XposedHelpers.findClass(
                "android.hardware.fingerprint.FingerprintManager.AuthenticationResult", classLoader);
        // 此方法 安卓 10 - 13 兼容
        XposedHelpers.findAndHookMethod(clazz, "onAuthenticationSucceeded",
                args0
                , new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
//                        XposedBridge.log(LOG_PREV + "指纹成功 ?! awake = " + HookConstant.awake);
                        // 拦截执行后 拿到外部类对象 重新监听指纹
                        Object this$0 = ReflexUtil.getField4Obj(param.thisObject, "this$0");
                        if (HookConstant.awake) {
//                            param.getResult();
                            HookConstant.authenticationSucceeded = false;
                        } else {
//                            XposedBridge.log(LOG_PREV + "么有解锁 因为手速不够 ");
                            param.setResult(null);
                            HookConstant.authenticationSucceeded = true;
                            HookConstant.userid = (int) ReflexUtil.runMethod(param.args[0], "getUserId", null);
                            HookConstant.times = System.currentTimeMillis();

                            // 如果超时事件过后还没有解锁 再进行监听
                            handler.postDelayed(() -> {
                                if (HookConstant.authenticationSucceeded) {
                                    if ( Build.VERSION.SDK_INT == 34 ) {
                                        startListeningForFingerprint(this$0);
                                    } else {
                                        ReflexUtil.runMethod(this$0, "startListeningForFingerprint", null);
                                    }
//                                    XposedBridge.log("重新监听指纹 == " + "startListeningForFingerprint");
                                }
                            }, HookConstant.TIMEOUT + 100);
                        }
                    }
                });
        // 此方法 todo 不知道是否兼容 10 / 11
        XposedHelpers.findAndHookMethod(clazz, "onAuthenticationFailed"
                , new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
//                        XposedBridge.log(LOG_PREV + "指纹失败 ?! awake = " + HookConstant.awake);
                        // 失败一直是false
                        HookConstant.authenticationSucceeded = false;
                        // 如果没醒着 拦截
                        if (!HookConstant.awake) {
                            param.setResult(null);
                            HookConstant.authenticationSucceeded = false;
                        }
                        // 尝试重置指纹解锁次数 按压指纹
                        Object this$0 = ReflexUtil.getField4Obj(param.thisObject, "this$0");
                        Object mFpm = ReflexUtil.getField4Obj(this$0, "mFpm");
                        if (mFpm != null) {
                            // todo 等待查看 FingerprintManager api getSensorProperties 只有12以上支持
                            int[] sensorIdList = getSensorIdList((FingerprintManager) mFpm, classLoader);
                            // 重置错误次数
                            resetLockoutError(sensorIdList, (FingerprintManager) mFpm, this$0);
                        }
                    }
                });
//        XposedBridge.log(LOG_PREV + " hook 完成 -- onAuthenticationSucceeded ");
    }


    @SuppressLint("MissingPermission")
    private void startListeningForFingerprint(Object obj) {
        Object mLogger = ReflexUtil.getField4Obj(obj, "mLogger");
        int mFingerprintRunningState = (int) ReflexUtil.getField4Obj(obj, "mFingerprintRunningState");

        final int userId = (int) ReflexUtil.runMethod(obj, "getCurrentUser", null);
        final boolean unlockPossible = (boolean) ReflexUtil.runMethod(obj, "isUnlockWithFingerprintPossible", new Object[]{userId}, int.class);
        /*if (ReflexUtil.getField4Obj(obj, "mFingerprintCancelSignal") != null) {
            mLogger.logUnexpectedFpCancellationSignalState(
                    mFingerprintRunningState,
                    unlockPossible);
        }*/

        if (mFingerprintRunningState == 2) {
            ReflexUtil.runMethod(obj, "setFingerprintRunningState", new Object[]{3}, int.class);
            return;
        }
        if (mFingerprintRunningState == 3) {
            // Waiting for restart via handleFingerprintError().
            return;
        }
        if (unlockPossible) {
            Object mFingerprintCancelSignal = new CancellationSignal();
            ReflexUtil.setField4Obj("mFingerprintCancelSignal", obj, mFingerprintCancelSignal);

//            boolean isUnlockingWithFingerprintAllowed = (boolean) ReflexUtil.runMethod(obj, "isUnlockingWithFingerprintAllowed", null);
            boolean isUnlockingWithFingerprintAllowed = true;
            FingerprintManager mFpm = (FingerprintManager) ReflexUtil.getField4Obj(obj, "mFpm");

            Object mFingerprintDetectionCallback
                    = ReflexUtil.getField4Obj(obj, "mFingerprintDetectionCallback");

            if (!isUnlockingWithFingerprintAllowed) {
//                mLogger.v("startListeningForFingerprint - detect");
                ReflexUtil.runMethod(mFpm, "detectFingerprint",
                        new Object[]{mFingerprintCancelSignal, mFingerprintDetectionCallback, userId}
                        , CancellationSignal.class, FingerprintManager.AuthenticationCallback.class, int.class
                        );
//                mFpm.detectFingerprint(
//                        mFingerprintCancelSignal,
//                        mFingerprintDetectionCallback,
//                        userId);
            } else {
//                mLogger.v("startListeningForFingerprint");
                FingerprintManager.AuthenticationCallback mFingerprintAuthenticationCallback
                        = (FingerprintManager.AuthenticationCallback) ReflexUtil.getField4Obj(obj, "mFingerprintAuthenticationCallback");

                ReflexUtil.runMethod(mFpm, "authenticate",
                        new Object[]{null, mFingerprintCancelSignal, mFingerprintAuthenticationCallback, null, userId}
                        , FingerprintManager.CryptoObject.class, CancellationSignal.class, FingerprintManager.AuthenticationCallback.class
                        , Handler.class, int.class
                );
            }
            ReflexUtil.runMethod(obj, "setFingerprintRunningState", new Object[]{1}, int.class);
        }
    }


    /**
     * 指纹错误的时候重置指纹错误累计
     *
     * @param sensorIdList 指纹 sensor id(s)
     * @param mFpm         FingerprintManager
     */
    private void resetLockoutError(int[] sensorIdList, FingerprintManager mFpm, Object this$0) {

        try {
            // 通知callback 已清除次数 以免显示次数过多
            Object resetTimeout;
            if (Build.VERSION.SDK_INT == 34) {
                resetTimeout = ReflexUtil.runMethod(this$0, "handleFingerprintLockoutReset", new Object[]{0}, int.class);
            } else {
                resetTimeout = ReflexUtil.runMethod(this$0, "handleFingerprintLockoutReset", null);
            }
            if (resetTimeout instanceof Exception) {
                XposedBridge.log(LOG_PREV + "尝试重置指纹解锁次数 失败");
                XposedBridge.log((Exception) resetTimeout);
            }
        } catch (Exception e) {
            XposedBridge.log(LOG_PREV + " 执行 handleFingerprintLockoutReset 错误");
            XposedBridge.log(e);
        }


        for (int sensorId : sensorIdList) {
            byte[] token = null; /* TODO: pass real auth token once fp HAL supports it */
            // 只有12以上支持
            Object resetLockout = ReflexUtil.runMethod(mFpm, "resetLockout",
                    new Object[]{sensorId, HookConstant.userid, token}
                    , int.class, int.class, byte[].class);
            // 安卓 10 / 11
            if (resetLockout instanceof Exception) {
                Object mService = ReflexUtil.getField4Obj(mFpm, "mService");
                ReflexUtil.runMethod(mService, "resetTimeout", new Object[]{null}, byte[].class);
            }
        }
    }

    /**
     * 获取当前设备的指纹传感器的传感器id
     *
     * @param mFpm        FingerprintManager
     * @param classLoader classLoader
     * @return r
     */
    private int[] getSensorIdList(FingerprintManager mFpm, ClassLoader classLoader) {

        int[] result = new int[]{0};
        try {
            @SuppressWarnings("unchecked")
            List<Object> getSensorProperties = (List<Object>) ReflexUtil.runMethod(mFpm, "getSensorProperties", null);
            if (getSensorProperties.size() > 0) {
                result = new int[getSensorProperties.size()];
            }
            Class<?> SensorPropertiesClass = XposedHelpers.findClass("android.hardware.biometrics.SensorProperties", classLoader);
            for (int i = 0; i < getSensorProperties.size(); i++) {
                Object getSensorId = ReflexUtil.runMethod(SensorPropertiesClass, getSensorProperties.get(i), "getSensorId", null);
                if (getSensorId != null) {
                    result[i] = (int) getSensorId;
                }
            }
            return result;
        } catch (Exception e) {
            return result;
        }
    }

}

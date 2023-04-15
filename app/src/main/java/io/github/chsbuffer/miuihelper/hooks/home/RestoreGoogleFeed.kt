package io.github.chsbuffer.miuihelper.hooks.home

import android.content.Intent
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import io.github.chsbuffer.miuihelper.model.BooleanDuringMethod
import io.github.chsbuffer.miuihelper.model.Hook
import miui.os.Build


object RestoreGoogleFeed : Hook() {
    override fun init(classLoader: ClassLoader) {
        if (!xPrefs.getBoolean("restore_google_feed", true) || Build.IS_INTERNATIONAL_BUILD) return

        // 启用可以切换负一屏（即“智能助理”或“Google”）
        val LauncherAssistantCompatClass =
            classLoader.loadClass("com.miui.home.launcher.LauncherAssistantCompat")
        XposedHelpers.setStaticBooleanField(
            LauncherAssistantCompatClass, "CAN_SWITCH_MINUS_SCREEN", true
        )

        // isUseGoogleMinusScreen, instead of LauncherAssistantCompatMIUI
        val mUtilities = classLoader.loadClass("com.miui.home.launcher.common.Utilities")
        XposedHelpers.findAndHookMethod("com.miui.home.launcher.LauncherAssistantCompat",
            classLoader,
            "newInstance",
            "com.miui.home.launcher.Launcher",
            object : BooleanDuringMethod(
                XposedHelpers.findClass(
                    "miui.os.Build", classLoader
                ), "IS_INTERNATIONAL_BUILD", true
            ) {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    super.value =
                        XposedHelpers.callStaticMethod(mUtilities, "getCurrentPersonalAssistant")
                            .equals("personal_assistant_google")
                    super.beforeHookedMethod(param)
                }
            })

        // 使用含 AssistantSwitchObserver 的 LauncherCallbacksGlobal, 而不是 LauncherCallbacksChinese （只观察负一屏是否开启）
        XposedHelpers.findAndHookConstructor(
            "com.miui.home.launcher.Launcher", classLoader, BooleanDuringMethod(
                XposedHelpers.findClass(
                    "miui.os.Build", classLoader
                ), "IS_INTERNATIONAL_BUILD", true
            )
        )

        // 恢复“切换负一屏”设置
        XposedHelpers.findAndHookMethod("com.miui.home.settings.MiuiHomeSettings",
            classLoader,
            "onCreatePreferences",
            android.os.Bundle::class.java,
            java.lang.String::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val mSwitchPersonalAssistant =
                        XposedHelpers.getObjectField(param.thisObject, "mSwitchPersonalAssistant")
                    XposedHelpers.callMethod(
                        mSwitchPersonalAssistant,
                        "setIntent",
                        Intent("com.miui.home.action.LAUNCHER_PERSONAL_ASSISTANT_SETTING")
                    )
                    XposedHelpers.callMethod(
                        mSwitchPersonalAssistant, "setOnPreferenceChangeListener", param.thisObject
                    )
                    val mPreferenceScreen =
                        XposedHelpers.callMethod(param.thisObject, "getPreferenceScreen")
                    XposedHelpers.callMethod(
                        mPreferenceScreen, "addPreference", mSwitchPersonalAssistant
                    )
                }
            })

        XposedHelpers.findAndHookMethod("com.miui.home.settings.MiuiHomeSettings",
            classLoader,
            "onResume",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val mSwitchPersonalAssistant =
                        XposedHelpers.getObjectField(param.thisObject, "mSwitchPersonalAssistant")
                    XposedHelpers.callMethod(
                        mSwitchPersonalAssistant, "setVisible", true
                    )

                }
            })

    }
}
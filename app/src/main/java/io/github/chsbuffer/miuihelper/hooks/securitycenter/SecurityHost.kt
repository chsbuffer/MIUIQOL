package io.github.chsbuffer.miuihelper.hooks.securitycenter

import android.app.Application
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import io.github.chsbuffer.miuihelper.model.Hook
import io.github.chsbuffer.miuihelper.util.XposedUtil.hooks
import java.lang.reflect.Method


object SecurityHost : Hook() {
    lateinit var app: Application
        private set

    val version: Int by lazy {
        app.packageManager.getPackageInfo(app.packageName, 0).versionCode
    }

    val versionName: String by lazy {
        app.packageManager.getPackageInfo(app.packageName, 0).versionName
    }

    val isInternational: Boolean by lazy {
        versionName[versionName.length - 3] == '1'
    }

    override fun init(classLoader: ClassLoader) {
        XposedHelpers.findAndHookMethod("com.miui.securitycenter.Application",
            classLoader,
            "onCreate",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val clz = classLoader.loadClass("com.miui.securitycenter.Application")
                    val m: Method = clz.declaredMethods.first { it.returnType == clz }

                    app = m.invoke(null) as Application

                    hooks(
                        classLoader,
                        RemoveBehaviorRecordWhiteListAndNoIgnoreSystemApp,
                        RemoveSetSystemAppWifiRuleAllow,
                        EnabledAllTextView,
                        LockOneHundred,
                        AppDetails
                    )
                }
            })
    }
}
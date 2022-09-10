package io.github.chsbuffer.miuihelper.hooks.securitycenter

import android.content.Context
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import io.github.chsbuffer.miuihelper.model.Hook
import io.github.chsbuffer.miuihelper.util.XposedUtil.hooks
import java.lang.reflect.Method


object Startup : Hook() {
    var version: Int = -1
        private set

    override fun init(classLoader: ClassLoader) {
        XposedHelpers.findAndHookMethod(
            "com.miui.securitycenter.Application",
            classLoader,
            "onCreate",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val clz = classLoader.loadClass("com.miui.securitycenter.Application")
                    val m: Method = clz.declaredMethods.first { it.returnType == clz }
                    val app: Context = m.invoke(null) as Context

                    getInfo(app)

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

    fun getInfo(app: Context) {
        version = app.packageManager.getPackageInfo(app.packageName, 0).versionCode
    }
}
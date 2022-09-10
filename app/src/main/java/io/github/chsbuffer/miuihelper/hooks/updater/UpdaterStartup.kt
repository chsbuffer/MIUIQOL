package io.github.chsbuffer.miuihelper.hooks.updater

import android.content.Context
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import io.github.chsbuffer.miuihelper.model.Hook
import io.github.chsbuffer.miuihelper.util.XposedUtil.hooks
import java.lang.reflect.Method

object UpdaterStartup : Hook() {
    var version: Int = -1
        private set

    override fun init(classLoader: ClassLoader) {
        val appClazz = XposedHelpers.findClass("com.android.updater.Application", classLoader)
        XposedHelpers.findAndHookMethod(
            appClazz,
            "onCreate",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val clz = XposedHelpers.findClass("android.content.Context", classLoader)
                    val m: Method = appClazz.declaredMethods.first { it.returnType == clz }
                    val app: Context = m.invoke(null) as Context

                    getInfo(app)

                    hooks(
                        classLoader,
                        RemoveOTAValidate
                    )
                }
            })
    }

    fun getInfo(app: Context) {
        version = app.packageManager.getPackageInfo(app.packageName, 0).versionCode
    }
}
package io.github.chsbuffer.miuihelper.hooks.updater

import android.app.Application
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import io.github.chsbuffer.miuihelper.model.Hook
import io.github.chsbuffer.miuihelper.util.XposedUtil.hooks
import java.lang.reflect.Method

object UpdaterHost : Hook() {
    lateinit var app: Application
        private set
    val version: Int by lazy {
        app.packageManager.getPackageInfo(app.packageName, 0).versionCode
    }

    override fun init(classLoader: ClassLoader) {
        val appClazz = XposedHelpers.findClass("com.android.updater.Application", classLoader)
        XposedHelpers.findAndHookMethod(appClazz, "onCreate", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val clz = XposedHelpers.findClass("android.content.Context", classLoader)
                val m: Method = appClazz.declaredMethods.first { it.returnType == clz }
                app = m.invoke(null) as Application

                hooks(
                    classLoader, RemoveOTAValidate
                )
            }
        })
    }

}
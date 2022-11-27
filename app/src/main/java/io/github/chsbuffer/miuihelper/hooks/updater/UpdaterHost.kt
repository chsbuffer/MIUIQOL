package io.github.chsbuffer.miuihelper.hooks.updater

import android.app.Application
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import io.github.chsbuffer.miuihelper.model.Hook
import io.github.chsbuffer.miuihelper.util.DexKitUtil
import io.github.chsbuffer.miuihelper.util.XposedUtil.hooks
import io.luckypray.dexkit.DexKitBridge
import java.lang.reflect.Method

object UpdaterHost : Hook() {
    lateinit var app: Application
        private set
    val version: Int by lazy {
        app.packageManager.getPackageInfo(app.packageName, 0).versionCode
    }

    lateinit var dexKit: DexKitBridge
    override fun init(classLoader: ClassLoader) {
        val appClazz = XposedHelpers.findClass("com.android.updater.Application", classLoader)
        XposedHelpers.findAndHookMethod(appClazz, "onCreate", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val clz = XposedHelpers.findClass("android.content.Context", classLoader)
                val m: Method = appClazz.declaredMethods.first { it.returnType == clz }
                app = m.invoke(null) as Application

                DexKitUtil.get(app).use {
                    if (it == null) {
                        XposedBridge.log("DexKitBridge create failed")
                        return
                    }
                    dexKit = it
                    hooks(
                        classLoader, RemoveOTAValidate
                    )
                }
            }
        })
    }

}
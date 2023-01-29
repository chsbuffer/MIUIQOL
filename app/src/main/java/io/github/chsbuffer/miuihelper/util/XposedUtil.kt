package io.github.chsbuffer.miuihelper.util

import android.app.Application
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import io.github.chsbuffer.miuihelper.model.Hook
import io.luckypray.dexkit.DexKitBridge

fun hooks(lpparam: LoadPackageParam, vararg hooks: Hook) {
    for (hook in hooks) {
        try {
            hook.init(lpparam)
        } catch (e: Exception) {
            XposedBridge.log(
                "Failed to do ${hook::class.java.simpleName} hook\n${e}"
            )
        } catch (e: Error) {
            XposedBridge.log(
                "Failed to do ${hook::class.java.simpleName} hook\n${e}"
            )
        }
    }
}

fun inContext(lpparam: LoadPackageParam, f: (Application) -> Unit) {
    val appClazz = XposedHelpers.findClass(lpparam.appInfo.className, lpparam.classLoader)
    XposedBridge.hookMethod(appClazz.getMethod("onCreate"), object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            val app = param.thisObject as Application
            f(app)
        }
    })
}

fun useDexKit(lpparam: LoadPackageParam, f: (DexKitBridge) -> Unit) {
    System.loadLibrary("dexkit")
    DexKitBridge.create(lpparam.appInfo.sourceDir)?.use {
        f(it)
    } ?: XposedBridge.log("DexKitBridge create failed")
}

package io.github.chsbuffer.miuihelper.util

import android.app.Application
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import io.github.chsbuffer.miuihelper.BuildConfig
import io.github.chsbuffer.miuihelper.model.Hook
import io.luckypray.dexkit.DexKitBridge
import io.luckypray.dexkit.descriptor.member.DexMethodDescriptor


fun dlog(text: String) {
    if (BuildConfig.DEBUG)
        XposedBridge.log("[MIUI QOL] " + text)
}

fun dlog(t: Throwable) {
    if (BuildConfig.DEBUG)
        XposedBridge.log(t)
}

fun hooks(lpparam: LoadPackageParam, vararg hooks: Hook) {
    hooks.forEach { hook ->
        runCatching {
            hook.init(lpparam)
        }.onFailure {
            XposedBridge.log("Failed to do ${hook::class.java.simpleName} hook\n${it}")
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

val dexKit: DexKitBridge
    get() = _dexKit!!
private var _dexKit: DexKitBridge? = null

fun useDexKit(lpparam: LoadPackageParam, f: (DexKitBridge) -> Unit) {
    System.loadLibrary("dexkit")
    DexKitBridge.create(lpparam.appInfo.sourceDir)?.use {
        _dexKit = it
        val ret = f(it)
        _dexKit = null
        return ret
    } ?: XposedBridge.log("DexKitBridge create failed")
}

val DexMethodDescriptor.method
    get() = "${this.name}${this.signature}"

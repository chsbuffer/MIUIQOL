package io.github.chsbuffer.miuihelper.util

import android.app.Application
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import io.github.chsbuffer.miuihelper.BuildConfig
import io.github.chsbuffer.miuihelper.model.Hook
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.result.MethodData
import kotlin.system.measureTimeMillis

fun dlog(text: String) {
    if (BuildConfig.DEBUG) XposedBridge.log("[MIUI QOL] " + text)
}

fun log(text: String) {
    XposedBridge.log("[MIUI QOL] " + text)
}

fun hooks(lpparam: LoadPackageParam, vararg hooks: Hook) {
    hooks.forEach { hook ->
        runCatching {
            hook.init(lpparam)
        }.onFailure {
            log("Failed to do ${hook::class.java.simpleName} hook\n${it}")
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
    runCatching {
        DexKitBridge.create(lpparam.appInfo.sourceDir)
    }.onSuccess {
        _dexKit = it
        f(it)
        _dexKit!!.close()
        _dexKit = null
    }.onFailure {
        log("DexKitBridge create failed for ${lpparam.packageName}")
    }
}


/** 记录错误，记录converter转换后的结果 */
inline fun <T> logSearch(name: String, converter: (T) -> String, f: () -> T): T {
    if (BuildConfig.DEBUG) {
        var timeInMillis: Long = 0
        return runCatching {
            val ret: T
            timeInMillis = measureTimeMillis {
                ret = f()
            }
            ret
        }.onSuccess {
            dlog("$name: ${converter(it)}\ttime cost: $timeInMillis ms")
        }.onFailure {
            log("find $name failed: $it")
        }.getOrThrow()
    } else {
        return runCatching(f).onFailure {
            log("find $name failed: $it")
        }.getOrThrow()
    }
}


val MethodData.method
    get() = "${this.name}${this.methodSign}"

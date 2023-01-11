package io.github.chsbuffer.miuihelper.model

import android.app.Application
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import io.github.chsbuffer.miuihelper.util.DexKitUtil
import io.github.chsbuffer.miuihelper.util.XposedUtil
import io.luckypray.dexkit.DexKitBridge

abstract class Host : Hook() {
    lateinit var app: Application
        private set

    lateinit var dexKit: DexKitBridge

    override fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        val appClazz = XposedHelpers.findClass(lpparam.appInfo.className, lpparam.classLoader)
        XposedBridge.hookMethod(appClazz.getMethod("onCreate"), object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                app = param.thisObject as Application

                DexKitUtil.get(app)?.also {
                    dexKit = it
                    XposedUtil.hooks(
                        lpparam, *hooks
                    )
                } ?: XposedBridge.log("DexKitBridge create failed")
            }
        })
    }

    abstract var hooks: Array<Hook>
}
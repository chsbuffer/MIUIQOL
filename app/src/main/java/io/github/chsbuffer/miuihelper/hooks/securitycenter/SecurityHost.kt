package io.github.chsbuffer.miuihelper.hooks.securitycenter

import android.app.Application
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import io.github.chsbuffer.miuihelper.model.Hook
import io.github.chsbuffer.miuihelper.util.DexKitUtil
import io.github.chsbuffer.miuihelper.util.XposedUtil.hooks
import io.luckypray.dexkit.DexKitBridge
import java.lang.reflect.Method


object SecurityHost : Hook() {
    lateinit var app: Application
        private set

    lateinit var dexKit: DexKitBridge

    override fun init(classLoader: ClassLoader) {
        XposedHelpers.findAndHookMethod("com.miui.securitycenter.Application",
            classLoader,
            "onCreate",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val clz = classLoader.loadClass("com.miui.securitycenter.Application")
                    val m: Method = clz.declaredMethods.first { it.returnType == clz }

                    app = m.invoke(null) as Application

                    DexKitUtil.get(app).use {
                        if (it == null) {
                            XposedBridge.log("DexKitBridge create failed")
                            return
                        }
                        dexKit = it

                        hooks(
                            classLoader,
                            RemoveBehaviorRecordWhiteListAndNoIgnoreSystemApp,
                            RemoveSetSystemAppWifiRuleAllow,
                            EnabledAllTextView,
                            LockOneHundred,
                            AppDetails
                        )
                    }
                }
            })
    }
}
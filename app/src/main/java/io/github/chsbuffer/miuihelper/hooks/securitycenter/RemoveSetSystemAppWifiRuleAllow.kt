package io.github.chsbuffer.miuihelper.hooks.securitycenter

import android.util.ArrayMap
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedHelpers
import io.github.chsbuffer.miuihelper.model.Hook


object RemoveSetSystemAppWifiRuleAllow : Hook() {
    override fun init() {
        if(!xPrefs.getBoolean("system_app_wlan_control",true))
            return

        XposedHelpers.findAndHookMethod(
            "com.miui.networkassistant.service.FirewallService",
            classLoader,
            "setSystemAppWifiRuleAllow",
            ArrayMap::class.java,
            XC_MethodReplacement.returnConstant(null)
        )
        XposedHelpers.findAndHookMethod(
            "com.miui.networkassistant.ui.fragment.ShowAppDetailFragment",
            classLoader,
            "initFirewallData",
            object : XC_MethodHook() {
                private var mAppInfo: Any? = null
                private var isSystemApp = false
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val obj = param.thisObject
                    mAppInfo = XposedHelpers.getObjectField(obj, "mAppInfo")
                    isSystemApp = XposedHelpers.getBooleanField(mAppInfo, "isSystemApp")
                    XposedHelpers.setBooleanField(mAppInfo, "isSystemApp", false)
                }

                override fun afterHookedMethod(param: MethodHookParam) {
                    XposedHelpers.setBooleanField(mAppInfo, "isSystemApp", isSystemApp)
                    mAppInfo = null
                }
            })

    }
}
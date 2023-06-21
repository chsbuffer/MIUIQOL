package io.github.chsbuffer.miuihelper.hooks.securitycenter

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import io.github.chsbuffer.miuihelper.model.Hook


class RemoveBehaviorRecordWhiteListAndNoIgnoreSystemApp(val dexKitCache: DexKitCache) : Hook() {
    val whiteApp = setOf(
        "com.miui.micloudsync",
        "com.mobiletools.systemhelper",
        "com.android.contacts",
        "com.android.phone",
        "com.android.mms",
        "com.android.providers.contacts",
        "com.android.calendar",
        "com.android.providers.calendar",
        "com.lbe.security.miui",
        "com.android.permissioncontroller",
        "com.android.incallui",
        "com.xiaomi.metoknlp",
        "com.xiaomi.location.fused",
        "android",
        "com.qualcomm.location",
        "com.xiaomi.bsp.gps.nps",
        "com.android.systemui",
        "com.google.android.gms",
    )

    override fun init(classLoader: ClassLoader) {
        if (!xPrefs.getBoolean("behavior_record_enhance", true)) return
        // 去除照明弹行为记录白名单

        // com.miui.permcenter.privacymanager.behaviorrecord

        val checkHook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                if (xPrefs.getBoolean("behavior_record_system_app_whitelist", true)) {
//                  不记录硬编码的系统应用，主要是系统组件
                    val str = param.args[1] as? String ?: return
                    val pkg = str.split('@')[0]

                    param.result = whiteApp.contains(pkg)
                } else {
//                  记录全部系统应用
                    param.result = false
                }
            }
        }

        // 跳过加载容许规则（云端+内置）
        XposedBridge.hookMethod(
            dexKitCache.behavior_initTolerateAppsMethod.getMethodInstance(classLoader),
            XC_MethodReplacement.returnConstant(null)
        )

        // 替换 check should ignore 方法
        XposedBridge.hookMethod(
            dexKitCache.behavior_shouldIgnore.getMethodInstance(classLoader), checkHook
        )
    }
}
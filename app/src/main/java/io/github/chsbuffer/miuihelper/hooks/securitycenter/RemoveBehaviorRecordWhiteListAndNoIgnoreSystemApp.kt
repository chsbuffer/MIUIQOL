package io.github.chsbuffer.miuihelper.hooks.securitycenter

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import io.github.chsbuffer.miuihelper.model.Hook


object RemoveBehaviorRecordWhiteListAndNoIgnoreSystemApp : Hook() {
    val banApp = hashSetOf(
        "com.android.camera",
        "com.miui.voiceassist",
        "com.xiaomi.aiasst.vision",
        "com.sohu.inputmethod.sogou.xiaomi",
        "com.android.quicksearchbox",
        "com.miui.personalassistant",
        "com.xiaomi.gamecenter.sdk.service",
        "com.miui.securityadd",
        "com.xiaomi.mircs",
        "com.google.android.gms",
        "com.xiaomi.mibrain.speech",
        "com.miui.yellowpage"
    )
    val whiteApp = hashSetOf(
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

        // 弃用：不读取内置白名单
        //  XC_MethodReplacement.returnConstant(null)
        /*
        inputStreamReader = miui.os.Build.IS_INTERNATIONAL_BUILD ? new InputStreamReader(context.getResources().getAssets().open("global_behavior_record_white.csv")) : new InputStreamReader(context.getResources().getAssets().open("behavior_record_white.csv"));
        */

        // 不读取白名单（云端+内置）
        /*
        Bundle call = context.getContentResolver().call(Uri.parse("content://com.miui.sec.THIRD_DESKTOP"), "getListForBehaviorWhite", (String) null, (Bundle) null);
        ...
        Log.e("BehaviorRecord-Utils", "getCloudBehaviorWhite:", e2);
        */

        val checkHook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                if (xPrefs.getBoolean("behavior_record_system_app_blacklist", false)) {
//                  仅记录硬编码的系统应用，主要是膨胀软件
                    val str = param.args[1] as? String ?: return
                    val pkg = str.split('@')[0]

                    if (banApp.contains(pkg)) {
                        param.result = false
                    }
                } else if (xPrefs.getBoolean("behavior_record_system_app_whitelist", true)) {
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

        /*
        Log.d("Enterprise", "Package " + str + "is ignored");
        ...
        return !com.miui.permcenter.privacymanager.l.c.a(context) ? UserHandle.getAppId(a2.applicationInfo.uid) < 10000 || (a2.applicationInfo.flags & 1) != 0 : !c(context, str2);
        */

        /* DexKit */
        SecurityHost.dexKit.batchFindMethodsUsingStrings(
            mapOf(
                "getCloudBehaviorWhite" to setOf(
                    "content://com.miui.sec.THIRD_DESKTOP", "getListForBehaviorWhite"
                ),
                "shouldIgnore" to setOf("Enterprise", "Package ", "is ignored")
            )
        ).forEach { (key, value) ->
            if (value.size == 1) {
                val m = value.first().getMethodInstance(classLoader)
                when (key) {
                    "getCloudBehaviorWhite" -> XposedBridge.hookMethod(m, XC_MethodReplacement.returnConstant(null))
                    "shouldIgnore" -> XposedBridge.hookMethod(m, checkHook)
                }
            } else {
                XposedBridge.log("locate not instance class: ${value.toList()}")
            }
        }
    }
}
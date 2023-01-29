package io.github.chsbuffer.miuihelper.hooks.securitycenter

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import io.github.chsbuffer.miuihelper.model.Hook
import io.luckypray.dexkit.DexKitBridge


class RemoveBehaviorRecordWhiteListAndNoIgnoreSystemApp(
    val dexKit: DexKitBridge
) : Hook() {
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

//  initTolerateApps, 初始化容忍应用
/*
    public static void a(Context context, boolean z) {
        Set<String> set;
        String str;
        List<String> a2 = a(context);           // getCloudBehaviorWhite 云端白名单
        if (a2 != null && a2.size() > 0) {
        ...
                }
                Log.i("BehaviorRecord-Utils", "initTolerateApps by cloud success");
            }
        }
        b(context);                             //  behavior_record_white.csv 本地白名单
    }
 */


//      shouldIgnore, 判断行为是否应被忽略
        /*
        Log.d("Enterprise", "Package " + str + "is ignored");
        ...
        return !com.miui.permcenter.privacymanager.l.c.a(context) ? UserHandle.getAppId(a2.applicationInfo.uid) < 10000 || (a2.applicationInfo.flags & 1) != 0 : !c(context, str2);
        */

        /* DexKit */
        dexKit.batchFindMethodsUsingStrings {
            addQuery("initTolerateApps", setOf("initTolerateApps by cloud success"))
            addQuery("shouldIgnore", setOf("Enterprise", "Package ", "is ignored"))
        }.forEach { (key, value) ->
            if (value.size == 1) {
                val m = value.first().getMethodInstance(classLoader)
                when (key) {
                    "initTolerateApps" -> XposedBridge.hookMethod(
                        m, XC_MethodReplacement.returnConstant(null)
                    )
                    "shouldIgnore" -> XposedBridge.hookMethod(m, checkHook)
                }
            } else {
                XposedBridge.log("locate not instance class: ${value.toList()}")
            }
        }
    }
}
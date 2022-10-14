package io.github.chsbuffer.miuihelper.hooks.securitycenter

import android.content.Context
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedHelpers
import io.github.chsbuffer.miuihelper.model.Hook


object RemoveBehaviorRecordWhiteListAndNoIgnoreSystemApp : Hook() {
    override fun init(classLoader: ClassLoader) {
        if (!xPrefs.getBoolean("behavior_record_enhance", true)) return
        // 去除照明弹行为记录白名单

        val clazzName = when {
            SecurityHost.isDev -> {
                when  {
                    SecurityHost.version >= 40000722 -> "f"
                    SecurityHost.version >= 40000714 -> "e"
                    else->"e"
                }
            }
            else -> "d"
        }


        val clazz = XposedHelpers.findClass(
            "com.miui.permcenter.privacymanager.behaviorrecord.$clazzName", classLoader
        )

        /*弃用：只 nop 不读取白名单（云端+内置）
        // 不读取内置白名单
        XposedHelpers.findAndHookMethod(
            clazz, "b",
            Context::class.java, XC_MethodReplacement.returnConstant(null)
        )
        /*
        inputStreamReader = miui.os.Build.IS_INTERNATIONAL_BUILD ? new InputStreamReader(context.getResources().getAssets().open("global_behavior_record_white.csv")) : new InputStreamReader(context.getResources().getAssets().open("behavior_record_white.csv"));
        */
         */

        // 不读取白名单（云端+内置）
        XposedHelpers.findAndHookMethod(
            clazz,
            "a",
            Context::class.java,
            Boolean::class.javaPrimitiveType,
            XC_MethodReplacement.returnConstant(null)
        )
        /*
        Bundle call = context.getContentResolver().call(Uri.parse("content://com.miui.sec.THIRD_DESKTOP"), "getListForBehaviorWhite", (String) null, (Bundle) null);
        ...
        Log.e("BehaviorRecord-Utils", "getCloudBehaviorWhite:", e2);
        */

        // 不忽略系统应用行为记录
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
        val checkHook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                if (xPrefs.getBoolean("behavior_record_system_app_blacklist", false)) {
//                  不容许（记录特定）系统应用
                    val str = param.args[1] as? String ?: return
                    val pkg = str.split('@')[0]

                    if (banApp.contains(pkg)) {
                        param.result = false
                    }
                } else if (xPrefs.getBoolean("behavior_record_system_app_whitelist", true)) {
//                  容许（不记录特定）系统应用
                    val str = param.args[1] as? String ?: return
                    val pkg = str.split('@')[0]

                    param.result = whiteApp.contains(pkg)
                } else {
//                  不容许（记录全部）系统应用
                    param.result = false
                }
            }
        }

        when {
            SecurityHost.isDev && SecurityHost.version >= 40000714 -> {
                XposedHelpers.findAndHookMethod(
                    clazz,
                    "a",
                    Context::class.java,
                    String::class.java,
                    Int::class.javaPrimitiveType,
                    Boolean::class.javaPrimitiveType,
                    checkHook
                )
            }
            else -> {
                XposedHelpers.findAndHookMethod(
                    clazz,
                    "a",
                    Context::class.java,
                    String::class.java,
                    Int::class.javaPrimitiveType,
                    checkHook
                )
            }
        }
        /*
        Log.d("Enterprise", "Package " + str + "is ignored");
        ...
        return !com.miui.permcenter.privacymanager.l.c.a(context) ? UserHandle.getAppId(a2.applicationInfo.uid) < 10000 || (a2.applicationInfo.flags & 1) != 0 : !c(context, str2);
        */
    }
}
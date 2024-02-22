package io.github.chsbuffer.miuihelper.hooks.home

import android.content.Context
import android.content.Intent
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import io.github.chsbuffer.miuihelper.model.Hook


object RestoreGoogleSearch : Hook() {
    override fun init() {
        if (!xPrefs.getBoolean("restore_google_search", false))
            return

        XposedHelpers.findAndHookMethod(
            "com.miui.home.launcher.SearchBarDesktopLayout",
            classLoader,
            "launchGlobalSearch",
            java.lang.String::class.java,
            java.lang.String::class.java,
            object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam) {
                    val context =
                        XposedHelpers.getObjectField(param.thisObject, "mLauncher") as Context
                    try {
                        context.startActivity(
                            Intent("android.search.action.GLOBAL_SEARCH").addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK
//                                        and Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        and Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                            ).setPackage("com.google.android.googlequicksearchbox")
                        )
                    } catch (e: Exception) {
                        // fallback doesn't work, still keep the code here because i don't care
                        XposedBridge.invokeOriginalMethod(
                            param.method,
                            param.thisObject,
                            param.args
                        )
                    }
                }
            }
        )
    }

}
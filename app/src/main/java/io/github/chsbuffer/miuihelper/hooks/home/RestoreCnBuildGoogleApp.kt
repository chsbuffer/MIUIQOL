package io.github.chsbuffer.miuihelper.hooks.home

import android.content.ComponentName
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import io.github.chsbuffer.miuihelper.model.Hook

object RestoreCnBuildGoogleApp : Hook() {
    override fun init(classLoader: ClassLoader) {
        if (!xPrefs.getBoolean("restore_google_icon", true))
            return

        XposedHelpers.findAndHookConstructor(
            "com.miui.home.launcher.AppFilter",
            classLoader,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val skippedItem = XposedHelpers.getObjectField(
                        param.thisObject,
                        "mSkippedItems"
                    ) as HashSet<ComponentName>

                    skippedItem.removeIf {
                        it.packageName == "com.google.android.googlequicksearchbox"
                                || it.packageName == "com.google.android.gms"
                    }
                }
            }
        )
    }

}
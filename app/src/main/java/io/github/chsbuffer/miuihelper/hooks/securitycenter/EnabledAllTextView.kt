package io.github.chsbuffer.miuihelper.hooks.securitycenter

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import io.github.chsbuffer.miuihelper.model.Hook

object EnabledAllTextView : Hook() {
    override fun init() {
        if (!xPrefs.getBoolean("enable_all_text_view", true))
            return
        // this exposed lots of disabled settings such as set system app wlan restrict

        XposedHelpers.findAndHookMethod(
            "android.widget.TextView",
            classLoader,
            "setEnabled",
            Boolean::class.javaPrimitiveType,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.args[0] = true
                }
            })
    }
}
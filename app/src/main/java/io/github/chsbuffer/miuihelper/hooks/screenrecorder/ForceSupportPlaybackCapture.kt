package io.github.chsbuffer.miuihelper.hooks.screenrecorder

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import io.github.chsbuffer.miuihelper.model.Hook

object ForceSupportPlaybackCapture : Hook() {
    override fun init() {
        if (!xPrefs.getBoolean("force_support_playbackcapture", true)) return

        XposedHelpers.findAndHookMethod("android.os.SystemProperties",
            classLoader,
            "getBoolean",
            String::class.java,
            Boolean::class.javaPrimitiveType,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (param.args[0] == "ro.vendor.audio.playbackcapture.screen")
                        param.result = true
                }
            })
    }
}
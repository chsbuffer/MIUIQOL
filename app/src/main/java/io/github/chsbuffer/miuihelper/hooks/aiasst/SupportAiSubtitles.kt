package io.github.chsbuffer.miuihelper.hooks.aiasst

import android.content.Context
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedHelpers
import io.github.chsbuffer.miuihelper.model.Hook


object SupportAiSubtitles : Hook() {
    override fun init(classLoader: ClassLoader) {
        if (!xPrefs.getBoolean("support_ai_subtitles", true))
            return
        val clazz = XposedHelpers.findClass(
            "com.xiaomi.aiasst.vision.utils.SupportAiSubtitlesUtils",
            classLoader
        )
        XposedHelpers.findAndHookMethod(
            clazz,
            "isSupportAiSubtitles",
            Context::class.java,
            XC_MethodReplacement.returnConstant(true)
        )
        XposedHelpers.findAndHookMethod(
            clazz,
            "isSupportJapanKoreaTranslation",
            Context::class.java,
            XC_MethodReplacement.returnConstant(true)
        )
        XposedHelpers.findAndHookMethod(
            clazz,
            "deviceWhetherSupportOfflineSubtitles",
            XC_MethodReplacement.returnConstant(true)
        )
        XposedHelpers.findAndHookMethod(
            clazz,
            "isSupportOfflineAiSubtitles",
            android.content.Context::class.java,
            XC_MethodReplacement.returnConstant(true)
        )
    }
}
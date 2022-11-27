package io.github.chsbuffer.miuihelper.hooks.systemui

import de.robv.android.xposed.XposedHelpers
import io.github.chsbuffer.miuihelper.model.Hook
import miui.os.Build

object NotificationSettingsNoWhiteList : Hook() {
    override fun init(classLoader: ClassLoader) {
        if (!xPrefs.getBoolean(
                "notification_settings_no_white_list",
                false
            ) || Build.IS_INTERNATIONAL_BUILD
        ) return

        XposedHelpers.setStaticBooleanField(
            XposedHelpers.findClass(
                "com.android.systemui.statusbar.notification.NotificationSettingsManager",
                classLoader
            ), "USE_WHITE_LISTS", false
        )
    }
}
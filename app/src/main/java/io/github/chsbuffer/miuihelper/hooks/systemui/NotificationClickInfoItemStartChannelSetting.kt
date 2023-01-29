package io.github.chsbuffer.miuihelper.hooks.systemui

import android.content.Context
import android.content.ContextHidden
import android.content.Intent
import android.os.Build
import android.os.UserHandleHidden
import android.provider.Settings
import android.service.notification.StatusBarNotification
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedHelpers
import dev.rikka.tools.refine.Refine
import io.github.chsbuffer.miuihelper.model.Hook


object NotificationClickInfoItemStartChannelSetting : Hook() {
    override fun init(classLoader: ClassLoader) {
        if (!xPrefs.getBoolean("notification_channel_setting", false))
            return

        var context: Context? = null
        var sbn: StatusBarNotification? = null

        XposedHelpers.findAndHookMethod("com.android.systemui.statusbar.notification.row.MiuiNotificationMenuRow",
            classLoader,
            "onClickInfoItem",
            Context::class.java,
            "com.android.systemui.plugins.statusbar.NotificationMenuRowPlugin\$MenuItem",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    sbn = XposedHelpers.getObjectField(
                        param.thisObject, "mSbn"
                    ) as StatusBarNotification?
                    context = XposedHelpers.getObjectField(
                        param.thisObject, "mContext"
                    ) as Context
                }

                override fun afterHookedMethod(param: MethodHookParam) {
                    sbn = null
                    context = null
                }
            })

        XposedHelpers.findAndHookMethod("com.android.systemui.statusbar.notification.NotificationSettingsHelper",
            classLoader,
            "startAppNotificationSettings",
            Context::class.java,
            String::class.java,
            String::class.java,
            Int::class.javaPrimitiveType,
            String::class.java,
            object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam): Any? {
                    startAppNotificationChannelSetting(context!!, sbn!!)
                    return null
                }
            })
    }

    private fun startAppNotificationChannelSetting(
        context: Context, sbn: StatusBarNotification
    ) {
        // https://cs.android.com/android/platform/superproject/+/master:packages/apps/Settings/src/com/android/settings/notification/history/NotificationSbnAdapter.java;l=132-137;drc=d21fbb5c0a53dc4127749c2c4b9e6f19e6dca128
        val intent: Intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).putExtra(
            Settings.EXTRA_APP_PACKAGE, sbn.packageName
        ).putExtra(
            Settings.EXTRA_CHANNEL_ID, sbn.notification.channelId
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            intent.putExtra(
                Settings.EXTRA_CONVERSATION_ID, sbn.notification.shortcutId
            )
        }

        Refine.unsafeCast<ContextHidden>(context)
            .startActivityAsUser(intent, UserHandleHidden.CURRENT)
    }
}
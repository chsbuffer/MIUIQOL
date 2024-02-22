package io.github.chsbuffer.miuihelper.hooks.systemui

import android.content.Context
import android.content.ContextHidden
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.UserHandleHidden
import android.provider.Settings
import android.service.notification.StatusBarNotification
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedHelpers
import dev.rikka.tools.refine.Refine
import io.github.chsbuffer.miuihelper.model.Hook


object NotificationClickInfoItemStartChannelSetting : Hook() {
    override fun init() {
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
                    val uid = param.args[3] as Int
                    startAppNotificationChannelSetting(context!!, sbn!!, uid)
                    return null
                }
            })
    }

    private fun startAppNotificationChannelSetting(
        context: Context, sbn: StatusBarNotification, uid: Int
    ) {
        val intent: Intent = Intent(Intent.ACTION_MAIN)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .setClassName(
                "com.android.settings",
                "com.android.settings.SubSettings"
            )
            .putExtra(
                // miui 自己修改了标准的 .notification.app 下的 ChannelNotificationSettings 代码后，
                // 拷贝了一份到 .notification 又稍作修改。在系统设置中实际使用了 .notification 的。
                // 并且"显示通知渠道设置"的 Xposed 模块作用的仅是 .notification 的。
                ":android:show_fragment",
                "com.android.settings.notification.ChannelNotificationSettings"
            )
            .putExtra(Settings.EXTRA_APP_PACKAGE, sbn.packageName)
            .putExtra(Settings.EXTRA_CHANNEL_ID, sbn.notification.channelId)
            .putExtra("app_uid", uid) // miui non-standard dual apps shit
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            intent.putExtra(Settings.EXTRA_CONVERSATION_ID, sbn.notification.shortcutId)
        }

        // workaround for miui 12.5, 金凡！！！
        val bundle = Bundle()
        bundle.putString(Settings.EXTRA_CHANNEL_ID, sbn.notification.channelId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            bundle.putString(Settings.EXTRA_CONVERSATION_ID, sbn.notification.shortcutId)
        }
        intent.putExtra(":android:show_fragment_args", bundle)

        Refine.unsafeCast<ContextHidden>(context)
            .startActivityAsUser(intent, UserHandleHidden.CURRENT)
    }
}
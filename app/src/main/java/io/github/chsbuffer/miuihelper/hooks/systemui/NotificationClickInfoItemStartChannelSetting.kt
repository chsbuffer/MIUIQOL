package io.github.chsbuffer.miuihelper.hooks.systemui

import android.content.Context
import android.content.ContextHidden
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.UserHandle
import android.os.UserHandleHidden
import android.provider.Settings
import android.service.notification.StatusBarNotification
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import dev.rikka.tools.refine.Refine
import io.github.chsbuffer.miuihelper.model.Hook
import io.github.chsbuffer.miuihelper.model.ScopedHook
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.result.MethodData

/*
* 更改记录：
* v1: MIUI 12 到 HyperOS 1.0 (A13)
* v2: HyperOS 1.0(A14)，r8 重拳出击，合并 lambda, 内联helper函数，呕
* */
class NotificationClickInfoItemStartChannelSetting(val dexKit: DexKitBridge) : Hook() {

    override fun init() {
        if (!xPrefs.getBoolean("notification_channel_setting", false)) return

        val startAppNotificationSettingsMethod = dexKit.findMethod {
            matcher {
                addEqString("startAppNotificationSettings pkg=%s label=%s uid=%s")
            }
        }.single()

        if (startAppNotificationSettingsMethod.methodName != "onClick") {
            hookV1()
        } else {
            hookV2(startAppNotificationSettingsMethod)
        }
    }

    private fun hookV1() {
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
                @Suppress("SameReturnValue")
                override fun replaceHookedMethod(param: MethodHookParam): Any? {
                    if (sbn!!.packageName == "com.miui.hybrid") return null

                    val intent = startAppNotificationChannelSetting(sbn!!)
                    Refine.unsafeCast<ContextHidden>(context)
                        .startActivityAsUser(intent, UserHandleHidden.CURRENT)
                    return null
                }
            })
    }

    private fun hookV2(startAppNotificationSettingsMethod: MethodData) {
        val startAppNotificationSettingsMethodInstance =
            startAppNotificationSettingsMethod.getMethodInstance(classLoader)
        // compiler merged closures. hope won't break soon.
        val r8Class = startAppNotificationSettingsMethod.declaredClass!!
        val menuRowField =
            r8Class.fields.single { it.typeName == "com.android.systemui.statusbar.notification.row.MiuiNotificationMenuRow" }

        var menuRow: Any?
        var context: Context? = null
        var sbn: StatusBarNotification? = null

        XposedBridge.hookMethod(
            startAppNotificationSettingsMethodInstance,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    menuRow = XposedHelpers.getObjectField(param.thisObject, menuRowField.fieldName)
                    context = XposedHelpers.getObjectField(menuRow, "mContext") as Context
                    sbn = XposedHelpers.getObjectField(menuRow, "mSbn") as StatusBarNotification
                }
            })

        val startActivityAsUser = XposedHelpers.findMethodExact(
            ContextWrapper::class.java,
            "startActivityAsUser",
            Intent::class.java,
            UserHandle::class.java
        )

        XposedBridge.hookMethod(
            startAppNotificationSettingsMethodInstance,
            ScopedHook(startActivityAsUser, object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam) {
                    if (sbn!!.packageName == "com.miui.hybrid") return

                    val intent = startAppNotificationChannelSetting(sbn)
                    XposedBridge.invokeOriginalMethod(
                        param.method, context, arrayOf(intent, UserHandleHidden.CURRENT)
                    )
                }
            })
        )
    }

    private fun startAppNotificationChannelSetting(
        sbn: StatusBarNotification
    ): Intent {
        val uid = XposedHelpers.getIntField(sbn, "mAppUid")
        val pkgName = sbn.packageName
        val channelId = sbn.notification.channelId
        val intent: Intent = Intent(Intent.ACTION_MAIN).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).setClassName(
                "com.android.settings", "com.android.settings.SubSettings"
            ).putExtra(
                // miui 自己修改了标准的 .notification.app 下的 ChannelNotificationSettings 代码后，
                // 拷贝了一份到 .notification 又稍作修改。在系统设置中实际使用了 .notification 的。
                // 并且"显示通知渠道设置"的 Xposed 模块作用的仅是 .notification 的。
                ":android:show_fragment",
                "com.android.settings.notification.ChannelNotificationSettings"
            ).putExtra(Settings.EXTRA_APP_PACKAGE, pkgName)
            .putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
            .putExtra("app_uid", uid) // miui non-standard dual apps shit

        // workaround for miui 12.5, 金凡！！！
        val bundle = Bundle()
        bundle.putString(Settings.EXTRA_CHANNEL_ID, channelId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            bundle.putString(Settings.EXTRA_CONVERSATION_ID, sbn.notification.shortcutId)
        }
        intent.putExtra(":android:show_fragment_args", bundle)

        return intent
    }
}
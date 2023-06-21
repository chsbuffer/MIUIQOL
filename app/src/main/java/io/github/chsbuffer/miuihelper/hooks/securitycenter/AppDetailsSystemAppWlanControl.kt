package io.github.chsbuffer.miuihelper.hooks.securitycenter

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.view.View
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import io.github.chsbuffer.miuihelper.BuildConfig
import io.github.chsbuffer.miuihelper.model.BooleanDuringMethod
import io.github.chsbuffer.miuihelper.model.Hook


class AppDetailsSystemAppWlanControl(val dexKitCache: DexKitCache, val app: Application) :
    Hook() {

    @SuppressLint("DiscouragedApi")
    override fun init(classLoader: ClassLoader) {
        if (!xPrefs.getBoolean("system_app_wlan_control", true)) return

        // init lazy
        dexKitCache.appDetails_isSystemAppField
        dexKitCache.appDetails_genNetCtrlSummaryMethod
        dexKitCache.appDetails_netCtrlShowDialogMethod
        dexKitCache.appDetails_OnLoadDataFinishMethod

        val appDetailClz =
            XposedHelpers.findClass("com.miui.appmanager.ApplicationsDetailsActivity", classLoader)

        val net_id = app.resources.getIdentifier("am_detail_net", "id", app.packageName)

        /* "联网控制"对话框确定 onClick */
        val saveNetCtrlDialogOnClickMethod = appDetailClz.declaredClasses.first {
            DialogInterface.OnMultiChoiceClickListener::class.java.isAssignableFrom(it)
        }.methods.first {
            it.name == "onClick"
        }

        XposedHelpers.findAndHookConstructor(appDetailClz, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                // 和 联网控制 有关的方法调用期间，将 isSystemApp 设为 false
                val hook = BooleanDuringMethod(
                    param.thisObject, dexKitCache.appDetails_isSystemAppField.name, false
                )

                XposedBridge.hookMethod(saveNetCtrlDialogOnClickMethod, hook)
                XposedBridge.hookMethod(
                    dexKitCache.appDetails_genNetCtrlSummaryMethod.getMethodInstance(classLoader),
                    hook
                )
                XposedBridge.hookMethod(
                    dexKitCache.appDetails_netCtrlShowDialogMethod.getMethodInstance(classLoader),
                    hook
                )
            }
        })

        // 仅WIFi设备会直接隐藏联网控制
        // Hook LiveData，当查看的应用有联网权限，显示”联网控制“
        if (XposedHelpers.callStaticMethod(
                classLoader.loadClass("android.os.SystemProperties"),
                "getBoolean",
                "ro.radio.noril",
                false
            ) as Boolean || BuildConfig.DEBUG
        ) {
            val net_ctrl_title_id = app.resources.getIdentifier(
                "app_manager_net_control_title", "string", app.packageName
            )
            val app_manager_disable = app.getString(
                app.resources.getIdentifier(
                    "app_manager_disable", "string", app.packageName
                )
            )

            XposedBridge.hookMethod(dexKitCache.appDetails_OnLoadDataFinishMethod.getMethodInstance(
                classLoader
            ), object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val pkgName =
                        (param.thisObject as Activity).intent.getStringExtra("package_name")!!
                    val allowInternet = app.packageManager.checkPermission(
                        "android.permission.INTERNET", pkgName
                    ) == PackageManager.PERMISSION_GRANTED

                    if (allowInternet && pkgName != "com.xiaomi.finddevice" && pkgName != "com.miui.mishare.connectivity") {
                        val netCtrlView = (param.thisObject as Activity).findViewById<View>(net_id)
                        netCtrlView.visibility = View.VISIBLE
                        XposedHelpers.callMethod(netCtrlView, "setTitle", net_ctrl_title_id)
                    }
                }
            })

            XposedBridge.hookMethod(dexKitCache.appDetails_genNetCtrlSummaryMethod.getMethodInstance(
                classLoader
            ),
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        // 生成 Summary 文本的方法未考虑仅WIFI设备的系统应用被禁用联网，会返回空白字符串
                        // 返回 "不允许" Resource ID
                        if ((param.result as String).isBlank()) param.result = app_manager_disable
                    }
                })
        }
    }
}
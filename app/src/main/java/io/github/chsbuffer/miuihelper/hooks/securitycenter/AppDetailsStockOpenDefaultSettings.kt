package io.github.chsbuffer.miuihelper.hooks.securitycenter

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.verify.domain.DomainVerificationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.View
import android.widget.TextView
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import io.github.chsbuffer.miuihelper.BuildConfig
import io.github.chsbuffer.miuihelper.R
import io.github.chsbuffer.miuihelper.model.Hook

class AppDetailsStockOpenDefaultSettings(val dexKitCache: DexKitCache, val app: Application) :
    Hook() {
    val domainVerificationManager: DomainVerificationManager by lazy(LazyThreadSafetyMode.NONE) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            app.getSystemService(
                DomainVerificationManager::class.java
            )
        } else {
            null!!
        }
    }
    val moduleContext: Context by lazy(LazyThreadSafetyMode.NONE) {
        app.createPackageContext(
            BuildConfig.APPLICATION_ID, 0
        )
    }

    @SuppressLint("DiscouragedApi")
    override fun init(classLoader: ClassLoader) {
        if (!xPrefs.getBoolean(
                "original_default_open_setting", true
            )
        ) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val default_id = app.resources.getIdentifier(
                "am_detail_default", "id", app.packageName
            )

            val appDetailClz =
                XposedHelpers.findClass(
                    "com.miui.appmanager.ApplicationsDetailsActivity",
                    classLoader
                )

            // 修改“清除默认操作”点击打开“默认打开”
            XposedHelpers.findAndHookMethod(
                appDetailClz,
                "onClick",
                View::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val clickedView = param.args[0]
                        val cleanDefaultView =
                            (param.thisObject as Activity).findViewById<View>(default_id)
                        val pkgName =
                            (param.thisObject as Activity).intent.getStringExtra("package_name")!!

                        if (clickedView == cleanDefaultView) {
                            val intent = Intent().apply {
                                action =
                                    Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS
                                addCategory(Intent.CATEGORY_DEFAULT)
                                data = Uri.parse("package:${pkgName}")
                                addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                                addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                            }
                            XposedHelpers.callMethod(
                                param.thisObject, "startActivity", intent
                            )
                            param.result = null
                        }
                    }
                })

            // 加载完毕数据后，修改“清除默认操作”按钮标题和描述为“默认打开”
            XposedBridge.hookMethod(dexKitCache.appDetails_OnLoadDataFinishMethod.getMethodInstance(
                classLoader
            ),
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val cleanDefaultView =
                            (param.thisObject as Activity).findViewById<View>(default_id)
                        val pkgName =
                            (param.thisObject as Activity).intent.getStringExtra("package_name")!!

                        val isLinkHandlingAllowed =
                            domainVerificationManager.getDomainVerificationUserState(
                                pkgName
                            )?.isLinkHandlingAllowed ?: false
                        val subTextId =
                            if (isLinkHandlingAllowed) R.string.app_link_open_always else R.string.app_link_open_never

                        // set title
                        // 因为 AppDetailTextBannerView 没有 setTitle 方法，
                        // 所以先将分别作为 Title 和 Summary 的两个 TextView 的文本都设为 "Open by default"
                        // 之后再调用 setSummary 设置 Summary 的 TextView
                        cleanDefaultView::class.java.declaredFields.forEach {
                            val textView = XposedHelpers.getObjectField(cleanDefaultView, it.name)
                            if (textView !is TextView) return@forEach

                            XposedHelpers.callMethod(
                                textView,
                                "setText",
                                arrayOf(CharSequence::class.java),
                                moduleContext.getString(R.string.open_by_default)
                            )
                        }

                        // set summary
                        XposedHelpers.callMethod(
                            cleanDefaultView, "setSummary", moduleContext.getString(subTextId)
                        )
                    }
                })
        }

    }
}
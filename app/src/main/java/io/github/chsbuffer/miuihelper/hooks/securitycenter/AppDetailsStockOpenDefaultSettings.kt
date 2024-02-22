package io.github.chsbuffer.miuihelper.hooks.securitycenter

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.verify.domain.DomainVerificationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import io.github.chsbuffer.miuihelper.BuildConfig
import io.github.chsbuffer.miuihelper.R
import io.github.chsbuffer.miuihelper.model.Hook


@SuppressLint("NewApi")
class AppDetailsStockOpenDefaultSettings(val dexKitCache: DexKitCache, val app: Application) :
    Hook() {
    val domainVerificationManager: DomainVerificationManager by lazy(LazyThreadSafetyMode.NONE) {
        app.getSystemService(
            DomainVerificationManager::class.java
        )
    }
    val moduleContext: Context by lazy(LazyThreadSafetyMode.NONE) {
        app.createPackageContext(
            BuildConfig.APPLICATION_ID, 0
        )
    }

    override fun init() {
        if (!xPrefs.getBoolean(
                "original_default_open_setting", true
            )
        ) return

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

        val appDetailClz = XposedHelpers.findClass(
            "com.miui.appmanager.ApplicationsDetailsActivity", classLoader
        )

        XposedHelpers.findAndHookMethod("com.miui.appmanager.ApplicationsDetailsActivity",
            classLoader,
            "onCreate",
            Bundle::class.java,
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun afterHookedMethod(param: MethodHookParam) {
                    findOpenDefaultText(param.thisObject as Activity)
                }
            })


        // 修改“清除默认操作”点击打开“默认打开”
        XposedHelpers.findAndHookMethod(appDetailClz,
            "onClick",
            View::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val clickedView = param.args[0]
                    val cleanDefaultView = getOpenDefaultView(param.thisObject as Activity)
                    val pkgName =
                        (param.thisObject as Activity).intent.getStringExtra("package_name")!!

                    if (clickedView == cleanDefaultView) {
                        val intent = Intent().apply {
                            action = Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS
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
        ), object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val cleanDefaultView = getOpenDefaultView(param.thisObject as Activity)
                val pkgName = (param.thisObject as Activity).intent.getStringExtra("package_name")!!

                setOpenDefaultViewText(cleanDefaultView, pkgName)
            }
        })
    }

    val openDefaultViewKey: String = "<open_default_view>"

    @SuppressLint("DiscouragedApi")
    fun findOpenDefaultText(activity: Activity) {
        val default_id = app.resources.getIdentifier(
            "am_detail_default", "id", app.packageName
        )

        var openDefaultView: View? = null
        if (default_id != 0) {
            openDefaultView = activity.findViewById(default_id)
        }

        if (openDefaultView == null) openDefaultView = createOpenDefaultView(activity)

        XposedHelpers.setAdditionalInstanceField(activity, openDefaultViewKey, openDefaultView)
    }

    fun createOpenDefaultView(activity: Activity): View {
        val appmanagerTextBannerViewClass = XposedHelpers.findClass(
            "com.miui.appmanager.widget.AppDetailTextBannerView", classLoader
        )

        val anotherTextBannerId = app.resources.getIdentifier(
            "am_global_perm", "id", app.packageName
        )
        val anotherTextBanner = activity.findViewById<LinearLayout>(anotherTextBannerId)

        val attributeSet = null
        val defaultView = XposedHelpers.newInstance(
            appmanagerTextBannerViewClass, activity, attributeSet
        ) as LinearLayout
        defaultView.setOnClickListener(activity as View.OnClickListener)
        copyLinearLayoutStyle(defaultView, anotherTextBanner)

        val insertAfterViewId = app.resources.getIdentifier("am_full_screen", "id", app.packageName)
        val insertAfterView = activity.findViewById<View>(insertAfterViewId)
        val viewGroup = insertAfterView.parent as ViewGroup
        viewGroup.addView(defaultView, viewGroup.indexOfChild(insertAfterView) + 1)

        return defaultView
    }

    private fun copyLinearLayoutStyle(thiz: LinearLayout, that: LinearLayout) {
        thiz.layoutParams = that.layoutParams
        thiz.setPadding(
            that.paddingLeft, that.paddingTop, that.paddingRight, that.paddingBottom
        )
        thiz.gravity = that.gravity
        thiz.orientation = that.orientation
    }

    fun getOpenDefaultView(activity: Activity): View {
        return XposedHelpers.getAdditionalInstanceField(activity, openDefaultViewKey) as View
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun setOpenDefaultViewText(cleanDefaultView: View, pkgName: String) {
        val isLinkHandlingAllowed = domainVerificationManager.getDomainVerificationUserState(
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
}
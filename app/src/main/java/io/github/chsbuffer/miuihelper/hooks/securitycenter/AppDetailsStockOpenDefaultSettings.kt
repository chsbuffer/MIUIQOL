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
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import io.github.chsbuffer.miuihelper.BuildConfig
import io.github.chsbuffer.miuihelper.R
import io.github.chsbuffer.miuihelper.model.Hook
import io.github.chsbuffer.miuihelper.util.dlog
import miuix.preference.TextPreference

/*
* 修改历史：
* v1: 6.x.x~7.x.x 针对 Activity 的【清除默认设置 am_detail_default: AppDetailTextBannerView】 重设文本，hook onClick 方法
* v2: 7.x.x 后期， 【清除默认设置】 被删除，自行构建 AppDetailTextBannerView
* v3: 10.x.x, 针对 androidx PreferenceFragmentCompat 的 app_default_pref，hook onPreferenceClick；原 Activity hook onClick 改为设置 onClickListener,
* */
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

    fun getOpenDefaultState(pkgName: String): String {
        val isLinkHandlingAllowed = domainVerificationManager.getDomainVerificationUserState(
            pkgName
        )?.isLinkHandlingAllowed ?: false
        val subTextId =
            if (isLinkHandlingAllowed) R.string.app_link_open_always else R.string.app_link_open_never
        return moduleContext.getString(subTextId)
    }

    fun getOpenDefaultTitle(): String = moduleContext.getString(R.string.open_by_default)

    companion object {
        @JvmStatic
        fun OpenDefaultOnClick(activity: Activity) {
            val pkgName = activity.intent.getStringExtra("package_name")!!
            dlog("open default: $pkgName")

            val intent = Intent().apply {
                action = Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS
                addCategory(Intent.CATEGORY_DEFAULT)
                data = Uri.parse("package:${pkgName}")
                addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            }
            activity.startActivity(intent)
        }
    }

    override fun init() {
        if (!xPrefs.getBoolean(
                "original_default_open_setting", true
            )
        ) return

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

        val appDetailsView = dexKitCache.appDetailsView.getInstance(classLoader)

        if (Activity::class.java.isAssignableFrom(appDetailsView)) {
            // v1, v2
            XposedBridge.hookMethod(dexKitCache.appDetails_OnLoadDataFinishMethod.getMethodInstance(
                classLoader
            ), object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    handleActivityOnLoadDataFinish(param.thisObject as Activity)
                }
            })
        } else {
            // v3
            XposedBridge.hookMethod(dexKitCache.appDetails_OnLoadDataFinishMethod.getMethodInstance(
                classLoader
            ), object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    handleFragmentOnLoadDataFinish(param.thisObject as PreferenceFragmentCompat)
                }
            })

            injectClassLoader()
            XposedHelpers.findAndHookMethod(appDetailsView,
                "onPreferenceClick",
                "androidx.preference.Preference",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val pref = param.args[0] as Preference
                        if (pref.key == "app_default_pref") {
                            val prefFrag = param.thisObject as PreferenceFragmentCompat
                            OpenDefaultOnClick(prefFrag.requireActivity())
                            param.result = true
                        }
                    }
                })
        }
    }

    // v1, v2
    fun handleActivityOnLoadDataFinish(activity: Activity) {
        var openDefaultView: View? = null
        val default_id = app.resources.getIdentifier(
            "am_detail_default", "id", app.packageName
        )
        if (default_id != 0) {
            openDefaultView = activity.findViewById(default_id)
        }
        // v2
        openDefaultView = openDefaultView ?: createOpenDefaultView(activity)
        openDefaultView.setOnClickListener { OpenDefaultOnClick(activity) }

        val pkgName = activity.intent.getStringExtra("package_name")!!
        // 加载完毕数据后，修改“清除默认操作”按钮标题和描述为“默认打开”
        setOpenDefaultViewText(openDefaultView, pkgName)
    }

    // v2
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
        copyLinearLayoutStyle(defaultView, anotherTextBanner)

        val insertAfterViewId = app.resources.getIdentifier("am_full_screen", "id", app.packageName)
        val insertAfterView = activity.findViewById<View>(insertAfterViewId)
        val viewGroup = insertAfterView.parent as ViewGroup
        viewGroup.addView(defaultView, viewGroup.indexOfChild(insertAfterView) + 1)

        return defaultView
    }

    // v2
    private fun copyLinearLayoutStyle(thiz: LinearLayout, that: LinearLayout) {
        thiz.layoutParams = that.layoutParams
        thiz.minimumHeight = that.minimumHeight
        thiz.background = that.background

        thiz.setPadding(
            that.paddingLeft, that.paddingTop, that.paddingRight, that.paddingBottom
        )
        thiz.gravity = that.gravity
        thiz.orientation = that.orientation
    }

    // v1, v2
    @RequiresApi(Build.VERSION_CODES.S)
    fun setOpenDefaultViewText(cleanDefaultView: View, pkgName: String) {
        // set title
        // 因为 AppDetailTextBannerView 没有 setTitle 方法，
        // 所以先将分别作为 Title 和 Summary 的两个 TextView 的文本都设为 "Open by default"
        // 之后再调用 setSummary 设置 Summary 的 TextView
        cleanDefaultView::class.java.declaredFields.forEach {
            val textView = XposedHelpers.getObjectField(cleanDefaultView, it.name)
            if (textView !is TextView) return@forEach

            XposedHelpers.callMethod(
                textView, "setText", arrayOf(CharSequence::class.java), getOpenDefaultTitle()
            )
        }

        // set summary
        XposedHelpers.callMethod(
            cleanDefaultView, "setSummary", getOpenDefaultState(pkgName)
        )
    }

    // v3
    fun handleFragmentOnLoadDataFinish(prefFrag: PreferenceFragmentCompat) {
        val activity = prefFrag.requireActivity()
        val pkgName = activity.intent.getStringExtra("package_name")!!
        val pref: TextPreference = prefFrag.findPreference("app_default_pref")!!
        // 加载完毕数据后，修改“清除默认操作”按钮标题和描述为“默认打开”
        pref.title = getOpenDefaultTitle()
        pref.summary = getOpenDefaultState(pkgName)
        dlog("handleFragment: $pkgName")
    }

    // v3, 为了模块加载宿主 androidx 和 miuix
    @SuppressLint("DiscouragedPrivateApi")
    fun injectClassLoader() {
        val self = this::class.java.classLoader!!
        val loader = self.parent
        val host = classLoader
        val sBootClassLoader: ClassLoader = Context::class.java.classLoader!!

        val fParent = ClassLoader::class.java.getDeclaredField("parent")
        fParent.setAccessible(true)
        fParent.set(self, object : ClassLoader(sBootClassLoader) {

            override fun findClass(name: String?): Class<*> {
                dlog("findClass $name")
                try {
                    return sBootClassLoader.loadClass(name)
                } catch (ignored: ClassNotFoundException) {
                }

                try {
                    return loader.loadClass(name)
                } catch (ignored: ClassNotFoundException) {
                }
                try {
                    return host.loadClass(name)
                } catch (ignored: ClassNotFoundException) {
                }

                throw ClassNotFoundException(name);
            }
        })
    }
}
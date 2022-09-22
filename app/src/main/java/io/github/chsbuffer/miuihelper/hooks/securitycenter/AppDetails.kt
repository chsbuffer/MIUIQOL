package io.github.chsbuffer.miuihelper.hooks.securitycenter

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.verify.domain.DomainVerificationManager
import android.os.Build
import android.widget.TextView
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import io.github.chsbuffer.miuihelper.BuildConfig
import io.github.chsbuffer.miuihelper.R
import io.github.chsbuffer.miuihelper.model.BooleanDuringMethod
import io.github.chsbuffer.miuihelper.model.Hook


object AppDetails : Hook() {
    data class Bean(
        /** *是否是系统应用* 字段 */
        val isSystemAppField: String,
//  this.o0 = (applicationInfo.flags & 1) != 0
        /** 获取 *联网控制* 按钮简介 方法 */
        val getNetCtrlSummaryMethod: String,
//  this.m.setSummary(L());               // L
//  ...
//  L() {                                 // o0 isSystemApp
//      if (this.o0) {
//          i2 = C0412R.string.app_manager_system_mobile_disable;
//      } else if (!this.q0) {
//          i2 = C0412R.string.app_manager_disable;
//      }

        /** *联网控制* 复选按钮 *点击事件* 处理 方法 */
        val NetCtrlDialogMethod: String,
//  Z();                                      // Z
//  str = "network_control";

        /** *包名* 字段 */
        val pkgNameField: String,
//  intent.putExtra("am_app_pkgname", ApplicationsDetailsActivity.this.b0);           // b0

        /** *清除默认操作* 按钮 字段
         *
         * *AppDetailTextBannerView*
         * */
        val cleanDefaultViewField: String,
        /** *LiveData* 读取后 View更新 方法 */
        val appDetailOnLoadDataFinishMethod: String,
        /** *LiveData* 读取后 View更新 方法参数 */
        val appDetailOnLoadDataFinishParameter: List<Any>,

//  public void a(a.j.b.c<Boolean> cVar, Boolean bool) {                      // a
//      ……
//      if (this.k0) {
//          appDetailTextBannerView = this.p;                                 // p cleanDefaultView
//          i2 = R.string.app_manager_default_open_summary;
//      } else {
//          appDetailTextBannerView = this.p;
//          i2 = R.string.app_manager_default_close_summary;
//      }
    ) {

        val domainVerificationManager: DomainVerificationManager by lazy {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SecurityHost.app.getSystemService(
                    DomainVerificationManager::class.java
                )
            } else {
                null!!
            }
        }
        val moduleContext: Context by lazy {
            SecurityHost.app.createPackageContext(
                BuildConfig.APPLICATION_ID, 0
            )
        }
    }

    override fun init(classLoader: ClassLoader) {

        val appDetailClz =
            XposedHelpers.findClass("com.miui.appmanager.ApplicationsDetailsActivity", classLoader)

        XposedHelpers.findAndHookMethod(appDetailClz, "initView", object : XC_MethodHook() {
            lateinit var bean: Bean

            override fun beforeHookedMethod(param: MethodHookParam) {
                bean = if (!SecurityHost.isInternational) SecurityHost.version.let {
                    when {
                        // 7.1.1~7.1.7 Dev
                        it >= 40000711 -> Bean(
                            "o0", "L", "b0", "c0", "p", "a",
                            listOf(
                                "c.n.b.c",
                                java.lang.Boolean::class.java,
                            ),
                        )

                        // 7.0.4 CN REL
                        it >= 40000704 -> Bean(
                            "o0", "J", "Z", "c0", "p", "a",
                            listOf(
                                "a.n.b.c",
                                java.lang.Boolean::class.java,
                            ),
                        )

                        // 6.2.0, 6.0.5 CN
                        it >= 40000605 -> Bean(
                            "n0", "I", "Z", "b0", "p", "a",
                            listOf(
                                "a.j.b.c",
                                java.lang.Boolean::class.java,
                            ),
                        )
                        else -> throw Exception("Not supported Security App ${SecurityHost.versionName}")
                    }
                } else SecurityHost.version.let {
                    when {
                        // 7.0.4 INTL REL
                        it >= 40000704 -> Bean(
                            "n0", "J", "Z", "b0", "p", "a",
                            listOf(
                                "c.o.b.c",
                                java.lang.Boolean::class.java,
                            ),
                        )

                        // 6.2.3, 6.3.0 INTL REL
                        it >= 40000623 -> Bean(
                            "n0", "I", "Z", "b0", "p", "a",
                            listOf(
                                "a.k.b.c",
                                java.lang.Boolean::class.java,
                            ),
                        )
                        else -> throw Exception("Not supported Security App ${SecurityHost.versionName}")
                    }
                }

                if (xPrefs.getBoolean("system_app_wlan_control", true)) {
                    // 和 联网控制 有关的方法调用期间，将 isSystemApp 设为 false
                    val hook = BooleanDuringMethod(param.thisObject, bean.isSystemAppField, false)

                    //region 联网控制多选对话框 onClick
                    appDetailClz.declaredClasses.first {
                        if (DialogInterface.OnMultiChoiceClickListener::class.java.isAssignableFrom(
                                it
                            )
                        ) {
                            val method = it.methods.firstOrNull { m -> m.name == "onClick" }

                            if (method != null) {
                                XposedBridge.hookMethod(method, hook)
                                return@first true
                            }
                        }
                        false
                    }
                        ?: throw Exception("OnMultiChoiceClickListener not found. wlan setting for System apps will not be available in app details")
                    //endregion

                    //region 联网控制获取概括文本方法
                    XposedHelpers.findAndHookMethod(
                        appDetailClz, bean.getNetCtrlSummaryMethod, hook
                    )

                    //endregion

                    //region 打开联网控制多选对话框方法
                    XposedHelpers.findAndHookMethod(appDetailClz, bean.NetCtrlDialogMethod, hook)

                    //endregion
                }

                // 修改“清除默认操作”点击打开“默认打开”
                if (xPrefs.getBoolean(
                        "original_default_open_setting", true
                    ) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                ) {
                    XposedHelpers.findAndHookMethod(appDetailClz,
                        "onClick",
                        android.view.View::class.java,
                        object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                val clickedView = param.args[0]
                                val cleanDefaultView = XposedHelpers.getObjectField(
                                    param.thisObject, bean.cleanDefaultViewField
                                )
                                val pkgName = XposedHelpers.getObjectField(
                                    param.thisObject, bean.pkgNameField
                                ) as String

                                if (clickedView == cleanDefaultView) {
                                    val intent = Intent().apply {
                                        action =
                                            android.provider.Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS
                                        addCategory(Intent.CATEGORY_DEFAULT)
                                        data = android.net.Uri.parse("package:${pkgName}")
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
                }

                // 修改“清除默认操作”按钮标题和描述为“默认打开”
                if (xPrefs.getBoolean(
                        "original_default_open_setting", true
                    ) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                ) {
                    XposedHelpers.findAndHookMethod(appDetailClz,
                        bean.appDetailOnLoadDataFinishMethod,
                        *bean.appDetailOnLoadDataFinishParameter.toTypedArray(),
                        object : XC_MethodHook() {
                            override fun afterHookedMethod(param: MethodHookParam) {
                                val cleanDefaultView = XposedHelpers.getObjectField(
                                    param.thisObject, bean.cleanDefaultViewField
                                )
                                val pkgName = XposedHelpers.getObjectField(
                                    param.thisObject, bean.pkgNameField
                                ) as String

                                val isLinkHandlingAllowed =
                                    bean.domainVerificationManager.getDomainVerificationUserState(
                                        pkgName
                                    )?.isLinkHandlingAllowed ?: false
                                val subTextId =
                                    if (isLinkHandlingAllowed) R.string.app_link_open_always else R.string.app_link_open_never

                                // set title
                                // 因为没有 setTitle(String)
                                // 所以将 AppDetailTextBannerView 的 TextView 文本都设为 "Open by default"
                                cleanDefaultView::class.java.declaredFields.forEach {
                                    val textView =
                                        XposedHelpers.getObjectField(cleanDefaultView, it.name)
                                    if (textView !is TextView) return@forEach

                                    XposedHelpers.callMethod(
                                        textView,
                                        "setText",
                                        arrayOf(CharSequence::class.java),
                                        bean.moduleContext.getString(R.string.open_by_default)
                                    )
                                }

                                // set summary
                                XposedHelpers.callMethod(
                                    cleanDefaultView,
                                    "setSummary",
                                    bean.moduleContext.getString(subTextId)
                                )
                            }
                        })
                }
            }

        })
    }
}
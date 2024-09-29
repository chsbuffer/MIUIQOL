package io.github.chsbuffer.miuihelper.hooks.securitycenter

import io.github.chsbuffer.miuihelper.util.dexKit
import io.github.chsbuffer.miuihelper.util.dlog
import io.github.chsbuffer.miuihelper.util.logSearch
import io.github.chsbuffer.miuihelper.util.method
import org.luckypray.dexkit.query.enums.OpCodeMatchType
import org.luckypray.dexkit.query.enums.StringMatchType

class DexKitCache {

    val appDetailsView by lazy(LazyThreadSafetyMode.NONE) {
        // getClassData 很便宜，不需要前置
        dexKit.getClassData("com.miui.appmanager.fragment.ApplicationsDetailsFragment") ?:
        dexKit.getClassData("com.miui.appmanager.ApplicationsDetailsActivity")!!
    }

    /** LiveData 读取后更新 View 的方法 */
    val appDetails_OnLoadDataFinishMethod by lazy(LazyThreadSafetyMode.NONE) {
        //
        //  public void a(a.j.b.c<Boolean> cVar, Boolean bool) {                      // <- a
        //      ……
        //      if (this.k0) {
        //          appDetailTextBannerView = this.p;
        //          i2 = R.string.app_manager_default_open_summary;
        //      } else {
        //          appDetailTextBannerView = this.p;
        //          i2 = R.string.app_manager_default_close_summary;
        //      }
        logSearch("appDetails_onLoadDataFinished", { it.method }) {
            appDetailsView.findMethod {
                matcher {
                    addEqString("enter_way")
                    returnType = "void"
                    paramTypes = listOf("", "")
                }
                findFirst = true
            }.single()
        }
    }

    /** "联网控制" 按钮生成 Summary 文本的方法 */
    val appDetails_genNetCtrlSummaryMethod by lazy(LazyThreadSafetyMode.NONE) {
        //
        //  this.m.setSummary(L());               // <- L
        //  ...
        //  L() {
        //      if (this.o0) {                    // o0 isSystemApp
        //          i2 = C0412R.string.app_manager_system_mobile_disable;
        //      } else if (!this.q0) {
        //          i2 = C0412R.string.app_manager_disable;
        //      }
        //  ...
        logSearch("appDetails_genNetCtrlSummaryMethod", { it.method }) {
            appDetailsView.findMethod {
                matcher {
                    opCodes(listOf(0x55, 0x39, 0x55, 0x39, 0x55, 0x38), OpCodeMatchType.StartsWith)
                    returnType = "java.lang.String"
                    paramTypes = listOf()
                }
            }.single()
        }
    }

    /** "联网控制" 按钮 onClick 处理方法 */
    val appDetails_netCtrlShowDialogMethod by lazy(LazyThreadSafetyMode.NONE) {
        //
        //  Z();                                      // <- Z
        //  str = "network_control";
        logSearch("appDetails_netCtrlShowDialogMethod", { it.method }) {
            appDetailsView.findMethod {
                matcher {
                    opCodes(listOf(0x55, 0x5c, 0x55, 0x5c, 0x55, 0x5c), OpCodeMatchType.StartsWith)
                    returnType = "void"
                    paramTypes = listOf()
                }
            }.single()
        }
    }

    /** "是否是系统应用" 字段 */
    val appDetails_isSystemAppField by lazy(LazyThreadSafetyMode.NONE) {
        appDetails_genNetCtrlSummaryMethod // 依赖项前置

        //
        //  initView() {
        //    ...
        //    this.o0 = (applicationInfo.flags & 1) != 0
        //    ...
        logSearch("appDetails_isSystemAppField", { it.name }) {
            appDetailsView.findField {
                matcher {
                    addWriteMethod("Lcom/miui/appmanager/ApplicationsDetailsActivity;->initView()V")
                    addReadMethod(appDetails_genNetCtrlSummaryMethod.descriptor)
                    type = "boolean"
                }
            }.single()
        }
    }

    private val behavior_FindMethodUsingStrings by lazy(LazyThreadSafetyMode.NONE) {
        logSearch("behavior_batchFindMethodUsingStrings", { "Success" }) {
            dexKit.batchFindMethodUsingStrings {
                addSearchGroup {
                    //
                    // public static void a(Context context, boolean z) {
                    //   Set<String> set;
                    //   String str;
                    //   List<String> a2 = a(context);           // getCloudBehaviorWhite 云端白名单
                    //   if (a2 != null && a2.size() > 0) {
                    //     ...
                    //       }
                    //       // 7.9.? 不再有此条日志，故查找 getCloudBehaviorWhite caller
                    //       Log.i("BehaviorRecord-Utils", "initTolerateApps by cloud success");
                    //     }
                    //   }
                    //   b(context);                             //  behavior_record_white.csv 本地白名单
                    // }
                    groupName = "getListForBehaviorWhite"
                    usingStrings(
                        listOf(
                            "content://com.miui.sec.THIRD_DESKTOP", "getListForBehaviorWhite"
                        ), StringMatchType.Equals
                    )
                }

                addSearchGroup {
                    // shouldIgnore, 判断行为是否应被忽略
                    //
                    //   Log.d("Enterprise", "Package " + str + "is ignored");
                    //   ...
                    //   return !com.miui.permcenter.privacymanager.l.c.a(context) ? UserHandle.getAppId(a2.applicationInfo.uid) < 10000 || (a2.applicationInfo.flags & 1) != 0 : !c(context, str2);

                    groupName = "shouldIgnore"
                    usingStrings(
                        listOf("Enterprise", "Package ", "is ignored"), StringMatchType.Equals
                    )
                }
            }
        }.also {
            dlog("behavior_getListForBehaviorWhite: " + it["getListForBehaviorWhite"]!!.single().descriptor)
            dlog("behavior_shouldIgnore: " + it["shouldIgnore"]!!.single().descriptor)
        }
    }

    /**  initTolerateApps, 初始化容忍应用 */
    val behavior_initTolerateAppsMethod by lazy(LazyThreadSafetyMode.NONE) {
        getListForBehaviorWhite // 依赖项前置

        logSearch("behavior_find_getListForBehaviorWhite_caller", { it.descriptor }) {
            getListForBehaviorWhite.declaredClass!!.findMethod {
                matcher {
                    addInvoke(getListForBehaviorWhite.descriptor)
                }
            }.single()
        }
    }

    private inline val getListForBehaviorWhite
        get() = behavior_FindMethodUsingStrings["getListForBehaviorWhite"]!!.single()

    val behavior_shouldIgnore
        get() = behavior_FindMethodUsingStrings["shouldIgnore"]!!.single()

}
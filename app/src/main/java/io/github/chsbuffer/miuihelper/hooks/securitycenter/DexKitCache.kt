package io.github.chsbuffer.miuihelper.hooks.securitycenter

import de.robv.android.xposed.XposedBridge
import io.github.chsbuffer.miuihelper.util.dexKit
import io.github.chsbuffer.miuihelper.util.dlog
import io.github.chsbuffer.miuihelper.util.method
import io.luckypray.dexkit.enums.FieldUsingType
import io.luckypray.dexkit.enums.MatchType

class DexKitCache {

    private inline fun <T> run(name: String, dlogf: (T) -> String, f: () -> T): T {
        return runCatching(f).onSuccess {
            dlog("$name: ${dlogf(it)}")
        }.onFailure {
            XposedBridge.log("[MIUI QOL] find $name failed.")
        }.getOrThrow()
    }

    private inline fun <T> run(name: String, f: () -> T): T {
        return runCatching(f).onFailure {
            XposedBridge.log("[MIUI QOL] find $name failed.")
        }.getOrThrow()
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
        run("appDetails_onLoadDataFinished",
            { it.method /* AppDetails 内查找的符号都在 ApplicationsDetailsActivity，所以不记录所在类 */ }) {
            dexKit.findMethodUsingString {
                usingString = "enter_way"
                matchType = MatchType.FULL
                methodDeclareClass = "Lcom/miui/appmanager/ApplicationsDetailsActivity;"
                methodReturnType = "void"
                methodParamTypes = arrayOf("", "")
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
        run("appDetails_genNetCtrlSummaryMethod", { it.method }) {
            dexKit.findMethodUsingOpCodeSeq {
                opSeq = intArrayOf(0x55, 0x39, 0x55, 0x39, 0x55, 0x38)
                methodDeclareClass = "Lcom/miui/appmanager/ApplicationsDetailsActivity;"
                methodReturnType = "Ljava/lang/String;"
                methodParamTypes = arrayOf()
            }.single()
        }
    }

    /** "联网控制" 按钮 onClick 处理方法 */
    val appDetails_netCtrlShowDialogMethod by lazy(LazyThreadSafetyMode.NONE) {
        //
        //  Z();                                      // <- Z
        //  str = "network_control";
        run("appDetails_netCtrlShowDialogMethod", { it.method }) {
            dexKit.findMethodUsingOpCodeSeq {
                opSeq = intArrayOf(0x55, 0x5c, 0x55, 0x5c, 0x55, 0x5c)
                methodDeclareClass = "Lcom/miui/appmanager/ApplicationsDetailsActivity;"
                methodReturnType = "V"
                methodParamTypes = arrayOf()
            }.single()
        }
    }

    /** "是否是系统应用" 字段 */
    val appDetails_isSystemAppField by lazy(LazyThreadSafetyMode.NONE) {
        //
        //  initView() {
        //    ...
        //    this.o0 = (applicationInfo.flags & 1) != 0
        //    ...
        val puts = run("appDetails_isSystemAppField_step1") {
            dexKit.findMethodUsingField {
                fieldType = "Z"
                usingType = FieldUsingType.PUT
                callerMethodDescriptor =
                    "Lcom/miui/appmanager/ApplicationsDetailsActivity;->initView()V"
            }.values.single()
        }

        val gets = run("appDetails_isSystemAppField_step2") {
            dexKit.findMethodUsingField {
                fieldType = "Z"
                usingType = FieldUsingType.GET
                callerMethodDescriptor = appDetails_genNetCtrlSummaryMethod.descriptor
            }.values.single()
        }

        puts.intersect(gets.toSet()).single().also {
            dlog("appDetails_isSystemAppField: ${it.name}")
        }
    }

    private val behavior_FindMethodUsingStrings by lazy(LazyThreadSafetyMode.NONE) {
        run("behavior_findMethodUsingStrings") {
            dexKit.batchFindMethodsUsingStrings {
                matchType = MatchType.FULL
                // getListForBehaviorWhite
                addQuery(
                    "getListForBehaviorWhite",
                    setOf("content://com.miui.sec.THIRD_DESKTOP", "getListForBehaviorWhite")
                )

                // shouldIgnore, 判断行为是否应被忽略
                //
                //   Log.d("Enterprise", "Package " + str + "is ignored");
                //   ...
                //   return !com.miui.permcenter.privacymanager.l.c.a(context) ? UserHandle.getAppId(a2.applicationInfo.uid) < 10000 || (a2.applicationInfo.flags & 1) != 0 : !c(context, str2);
                addQuery("shouldIgnore", setOf("Enterprise", "Package ", "is ignored"))
            }
        }
    }


    /**  initTolerateApps, 初始化容忍应用 */
    val behavior_initTolerateAppsMethod by lazy(LazyThreadSafetyMode.NONE) {
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
        val getListForBehaviorWhite = run("behavior_getListForBehaviorWhite", { it.descriptor }) {
            behavior_FindMethodUsingStrings["getListForBehaviorWhite"]!!.single()
        }

        run("behavior_find_getListForBehaviorWhite_caller", { it.descriptor }) {
            dexKit.findMethodCaller {
                methodDescriptor = getListForBehaviorWhite.descriptor
            }.keys.first()
        }
    }

    val behavior_shouldIgnore by lazy(LazyThreadSafetyMode.NONE) {
        run("behavior_shouldIgnore", { it.descriptor }) {
            behavior_FindMethodUsingStrings["shouldIgnore"]!!.single()
        }
    }

}
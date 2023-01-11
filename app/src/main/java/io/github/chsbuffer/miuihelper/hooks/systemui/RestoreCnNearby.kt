package io.github.chsbuffer.miuihelper.hooks.systemui

import android.content.pm.ApplicationInfo
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import io.github.chsbuffer.miuihelper.model.BooleanDuringMethod
import io.github.chsbuffer.miuihelper.model.Hook
import miui.os.Build

object RestoreCnNearby : Hook() {
    var isHooked = false

    override fun init(classLoader: ClassLoader) {
        if (!xPrefs.getBoolean("restore_nearby_sharing_tile", true)) return

        /**/
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            XposedHelpers.findClass(
                "com.android.systemui.shared.plugins.PluginInstance\$Factory", classLoader
            ).getDeclaredMethod(
                "getClassLoader", ApplicationInfo::class.java, ClassLoader::class.java
            )
        } else {
            // https://github.com/KieronQuinn/ClassicPowerMenu/blob/2e1648316b7bf1f5786e5d1132dc081436375c08/app/src/main/java/com/kieronquinn/app/classicpowermenu/components/xposed/Xposed.kt#L118
            XposedHelpers.findClass(
                "com.android.systemui.shared.plugins.PluginManagerImpl", classLoader
            ).getDeclaredMethod(
                "getClassLoader", ApplicationInfo::class.java
            )
        }.let {
            XposedBridge.hookMethod(it, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val applicationInfo = param.args[0] as ApplicationInfo
                    val pluginClassLoader = param.result as? ClassLoader ?: return
                    if (applicationInfo.packageName != "miui.systemui.plugin" || isHooked) return

                    XposedHelpers.findAndHookMethod(
                        "miui.systemui.controlcenter.qs.customize.TileQueryHelper\$Companion",
                        pluginClassLoader,
                        "filterNearby",
                        String::class.java,
                        XC_MethodReplacement.returnConstant(false)
                    )
                    isHooked = true
                }
            })
        }

        val controlCenterUtilsClazz = XposedHelpers.findClass(
            "com.android.systemui.controlcenter.utils.ControlCenterUtils", classLoader
        )

        if (!Build.IS_INTERNATIONAL_BUILD) {
            val spoofGlobal = BooleanDuringMethod(
                XposedHelpers.findClass(
                    "com.android.systemui.controlcenter.utils.Constants", classLoader
                ), "IS_INTERNATIONAL", true
            )

            /**/
            XposedHelpers.findClassIfExists(
                "com.android.systemui.controlcenter.qs.MiuiQSTileHostInjector", classLoader
            ) ?: XposedHelpers.findClass(
                "com.android.systemui.qs.MiuiQSTileHostInjector", classLoader
            ).let {
                XposedHelpers.findAndHookMethod(it, "createMiuiTile", String::class.java, spoofGlobal)
            }

            /**/
            XposedHelpers.findMethodExactIfExists(
                controlCenterUtilsClazz, "filterCustomTile", String::class.java
            )?.let { XposedBridge.hookMethod(it, spoofGlobal) }
        }

        /**/
        XposedHelpers.findMethodExactIfExists(
            controlCenterUtilsClazz, "filterNearby", String::class.java
        )?.let {
            XposedBridge.hookMethod(it, XC_MethodReplacement.returnConstant(false))
        }
    }
}
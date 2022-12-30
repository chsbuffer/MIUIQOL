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
        val m = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val pluginManagerImplClass = XposedHelpers.findClass(
                "com.android.systemui.shared.plugins.PluginInstance\$Factory", classLoader
            )
            pluginManagerImplClass.getDeclaredMethod(
                "getClassLoader", ApplicationInfo::class.java, ClassLoader::class.java
            )
        } else {
            // https://github.com/KieronQuinn/ClassicPowerMenu/blob/2e1648316b7bf1f5786e5d1132dc081436375c08/app/src/main/java/com/kieronquinn/app/classicpowermenu/components/xposed/Xposed.kt#L118
            val pluginManagerImplClass = XposedHelpers.findClass(
                "com.android.systemui.shared.plugins.PluginManagerImpl", classLoader
            )
            pluginManagerImplClass.getDeclaredMethod(
                "getClassLoader", ApplicationInfo::class.java
            )
        }

        XposedBridge.hookMethod(m, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val applicationInfo = param.args[0] as ApplicationInfo
                val pluginClassLoader = param.result as? ClassLoader ?: return
                if (applicationInfo.packageName != "miui.systemui.plugin" || isHooked) return

                XposedHelpers.findAndHookMethod("miui.systemui.controlcenter.qs.customize.TileQueryHelper\$Companion",
                    pluginClassLoader,
                    "filterNearby",
                    String::class.java,
                    object : XC_MethodReplacement() {
                        override fun replaceHookedMethod(param: MethodHookParam): Any {
                            return false
                        }
                    })
                isHooked = true
            }
        })

        /**/
        if (Build.IS_INTERNATIONAL_BUILD) return

        val hook = BooleanDuringMethod(
            XposedHelpers.findClass(
                "com.android.systemui.controlcenter.utils.Constants", classLoader
            ), "IS_INTERNATIONAL", true
        )

        /**/
        val injector = XposedHelpers.findClassIfExists(
            "com.android.systemui.controlcenter.qs.MiuiQSTileHostInjector", classLoader
        ) ?: XposedHelpers.findClass(
            "com.android.systemui.qs.MiuiQSTileHostInjector", classLoader
        )
        XposedHelpers.findAndHookMethod(
            injector, "createMiuiTile", String::class.java, hook
        )

        /**/
        val controlCenterUtilsClazz = XposedHelpers.findClass(
            "com.android.systemui.controlcenter.utils.ControlCenterUtils", classLoader
        )
        val m1 = XposedHelpers.findMethodExactIfExists(
            controlCenterUtilsClazz, "filterNearby", String::class.java
        )
        if (m1 != null) {
            XposedBridge.hookMethod(m1, XC_MethodReplacement.returnConstant(false))
        } else {
            XposedHelpers.findAndHookMethod(
                controlCenterUtilsClazz, "filterCustomTile", String::class.java, hook
            )
        }
    }
}
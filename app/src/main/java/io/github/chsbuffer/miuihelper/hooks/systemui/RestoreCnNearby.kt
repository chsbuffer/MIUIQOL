package io.github.chsbuffer.miuihelper.hooks.systemui

import android.content.pm.ApplicationInfo
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import io.github.chsbuffer.miuihelper.model.BooleanDuringMethod
import io.github.chsbuffer.miuihelper.model.Hook

object RestoreCnNearby : Hook() {
    var isHooked = false

    override fun init(classLoader: ClassLoader) {
        if (!xPrefs.getBoolean("restore_nearby_sharing_tile", true))
            return

        val hook = BooleanDuringMethod(
            XposedHelpers.findClass(
                "com.android.systemui.controlcenter.utils.Constants",
                classLoader
            ),
            "IS_INTERNATIONAL",
            true
        )

        // https://github.com/KieronQuinn/ClassicPowerMenu/blob/2e1648316b7bf1f5786e5d1132dc081436375c08/app/src/main/java/com/kieronquinn/app/classicpowermenu/components/xposed/Xposed.kt#L118
        val pluginManagerImplClass = XposedHelpers.findClass(
            "com.android.systemui.shared.plugins.PluginManagerImpl",
            classLoader
        )
        val m =
            pluginManagerImplClass.getDeclaredMethod("getClassLoader", ApplicationInfo::class.java)
        XposedBridge.hookMethod(m, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val applicationInfo = param.args[0] as ApplicationInfo
                val pluginClassLoader = param.result as? ClassLoader ?: return
                if (applicationInfo.packageName != "miui.systemui.plugin" || isHooked) return

                XposedHelpers.findAndHookMethod(
                    "miui.systemui.controlcenter.qs.customize.TileQueryHelper\$Companion",
                    pluginClassLoader,
                    "filterNearby",
                    String::class.java,
                    object : XC_MethodReplacement() {
                        override fun replaceHookedMethod(param: MethodHookParam): Any {
                            return false
                        }
                    }
                )
                isHooked = true
            }
        })

        /**/
        XposedHelpers.findAndHookMethod(
            "com.android.systemui.controlcenter.qs.MiuiQSTileHostInjector",
            classLoader,
            "createMiuiTile",
            String::class.java,
            hook
        )

        /**/
        val controlCenterUtilsClazz = XposedHelpers.findClass(
            "com.android.systemui.controlcenter.utils.ControlCenterUtils",
            classLoader
        )
        val m1 = XposedHelpers.findMethodExact(
            controlCenterUtilsClazz,
            "filterNearby",
            String::class.java
        )
        if (m1 != null) {
            XposedBridge.hookMethod(m1, XC_MethodReplacement.returnConstant(false))
        } else {
            XposedHelpers.findAndHookMethod(
                controlCenterUtilsClazz,
                "filterCustomTile",
                String::class.java,
                hook
            )
        }
    }
}
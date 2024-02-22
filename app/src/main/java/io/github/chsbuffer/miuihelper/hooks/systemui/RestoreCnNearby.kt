package io.github.chsbuffer.miuihelper.hooks.systemui

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import io.github.chsbuffer.miuihelper.model.BooleanDuringMethod
import io.github.chsbuffer.miuihelper.model.Hook
import io.github.chsbuffer.miuihelper.util.dlog
import io.luckypray.dexkit.DexKitBridge
import io.luckypray.dexkit.enums.MatchType
import miui.os.Build
import java.lang.reflect.Method

class RestoreCnNearby(val dexKit: DexKitBridge) : Hook() {
    var isHooked = false

    override fun init() {
        if (!xPrefs.getBoolean("restore_nearby_sharing_tile", true)) return

        /* hook miui.systemui.plugin */
        val getPluginClassLoaderMethod = findGetPluginClassLoaderMethod()
        XposedBridge.hookMethod(getPluginClassLoaderMethod, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val pluginClassLoader = param.result as? ClassLoader ?: return
                if (isHooked) return

                val filterNearbyMethod = XposedHelpers.findMethodExactIfExists(
                    "miui.systemui.controlcenter.qs.customize.TileQueryHelper\$Companion",
                    pluginClassLoader,
                    "filterNearby",
                    String::class.java
                ) ?: return

                XposedBridge.hookMethod(
                    filterNearbyMethod, XC_MethodReplacement.returnConstant(false)
                )
                isHooked = true
            }
        })


        /* hook systemui */
        val controlCenterUtilsClazz = XposedHelpers.findClass(
            "com.android.systemui.controlcenter.utils.ControlCenterUtils", classLoader
        )

        if (!Build.IS_INTERNATIONAL_BUILD) {
            dexKit.findMethodUsingString {
                usingString = "custom(com.google.android.gms/.nearby.sharing.SharingTileService)"
                matchType = MatchType.FULL
            }.forEach { m ->
                dlog("createMiuiTile is: ${m.descriptor}")

                val isInternational = dexKit.findMethodUsingField {
                    fieldType = "Z"
                    callerMethodDescriptor = m.descriptor
                }.values.single().first { f -> f.name.contains("INTERNATIONAL") }
                    .also { f -> dlog("createMiuiTile using IS_INTERNATIONAL field: ${f.descriptor}") }

                val hook = BooleanDuringMethod(
                    XposedHelpers.findClass(isInternational.declaringClassName, classLoader),
                    isInternational.name,
                    true
                )
                XposedBridge.hookMethod(m.getMethodInstance(classLoader), hook)
            }
        }

        //
        XposedHelpers.findMethodExactIfExists(
            controlCenterUtilsClazz, "filterNearby", String::class.java
        )?.let {
            XposedBridge.hookMethod(it, XC_MethodReplacement.returnConstant(false))
        }
    }

    private fun findGetPluginClassLoaderMethod(): Method {
        // HyperOS 1.0
        // MIUI 14 com.android.systemui.shared.plugins.PluginInstance$Factory.getClassLoader(android.content.pm.ApplicationInfo, java.lang.ClassLoader)
        val methodDescriptor = dexKit.findMethodUsingString {
            usingString = "Cannot get class loader for non-privileged plugin. Src:"
            matchType = MatchType.FULL
        }.firstOrNull() ?:
        // MIUI 13 com.android.systemui.shared.plugins.PluginManagerImpl.getClassLoader(android.content.pm.ApplicationInfo)
        dexKit.findMethodUsingString {
            usingString = "Cannot get class loader for non-whitelisted plugin. Src:"
            matchType = MatchType.FULL
        }.firstOrNull() ?:
        // oops
        throw Exception("SystemUI plugin loading method not found.")

        return methodDescriptor.let {
            dlog("System UI plugin loader method is: ${it.descriptor}")
            it.getMethodInstance(classLoader)
        }
    }
}
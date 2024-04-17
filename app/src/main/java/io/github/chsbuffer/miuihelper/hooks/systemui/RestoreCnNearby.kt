package io.github.chsbuffer.miuihelper.hooks.systemui

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import io.github.chsbuffer.miuihelper.model.BooleanDuringMethod
import io.github.chsbuffer.miuihelper.model.Hook
import io.github.chsbuffer.miuihelper.util.dlog
import io.github.chsbuffer.miuihelper.util.method
import miui.os.Build
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.query.enums.StringMatchType
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
            dexKit.findMethod {
                matcher {
                    addUsingString(
                        "custom(com.google.android.gms/.nearby.sharing.SharingTileService)",
                        StringMatchType.Equals
                    )
                }
            }.forEach { createMiuiTileMethodData ->
                dlog("createMiuiTile is: ${createMiuiTileMethodData.method}")

                dexKit.findField {
                    matcher {
                        type = "boolean"
                        addReadMethod(createMiuiTileMethodData.descriptor)
                        name("INTERNATIONAL", StringMatchType.Contains)
                    }
                    findFirst = true
                }.single().also { isInternationalFieldData ->
                    dlog("${createMiuiTileMethodData.method} using IS_INTERNATIONAL field: ${isInternationalFieldData.descriptor}")

                    val hook = BooleanDuringMethod(
                        XposedHelpers.findClass(
                            isInternationalFieldData.declaredClassName, classLoader
                        ), isInternationalFieldData.name, true
                    )
                    XposedBridge.hookMethod(
                        createMiuiTileMethodData.getMethodInstance(classLoader), hook
                    )
                }
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
        val methodDescriptor = dexKit.batchFindMethodUsingStrings {
            addSearchGroup {
                // HyperOS 1.0: maybe somewhere else, don't care, just do dexKit search.
                // MIUI 14: com.android.systemui.shared.plugins.PluginInstance$Factory.getClassLoader(android.content.pm.ApplicationInfo, java.lang.ClassLoader)
                groupName = "MIUI 14"
                usingStrings(
                    listOf("Cannot get class loader for non-privileged plugin. Src:"),
                    StringMatchType.Equals
                )
            }
            addSearchGroup {
                // MIUI 13 com.android.systemui.shared.plugins.PluginManagerImpl.getClassLoader(android.content.pm.ApplicationInfo)
                groupName = "MIUI 13"
                usingStrings(
                    listOf("Cannot get class loader for non-whitelisted plugin. Src:"),
                    StringMatchType.Equals
                )
            }
        }.values.flatten().singleOrNull()
            ?.also { dlog("System UI plugin loader method is: ${it.descriptor}") }
            ?: throw Exception("System UI plugin loader method not found.")
        return methodDescriptor.getMethodInstance(classLoader)
    }
}
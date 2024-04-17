package io.github.chsbuffer.miuihelper.hooks.securitycenter

import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import io.github.chsbuffer.miuihelper.BuildConfig
import io.github.chsbuffer.miuihelper.model.BooleanDuringMethod
import io.github.chsbuffer.miuihelper.model.Hook
import miui.os.Build

class IntlEnableBehaviorRecord(val dexKitCache: DexKitCache) : Hook() {
    override fun init() {
        if ((!xPrefs.getBoolean("behavior_record_enhance", true)
                    || !Build.IS_INTERNATIONAL_BUILD)
            && !BuildConfig.DEBUG
        ) return

        val spoofCN = BooleanDuringMethod(
            XposedHelpers.findClass(
                "miui.os.Build", classLoader
            ), "IS_INTERNATIONAL_BUILD", false
        )

        dexKitCache.behavior_shouldIgnore.declaredClass!!.findMethod {
            matcher {
                addUsingField("Lmiui/os/Build;->IS_INTERNATIONAL_BUILD:Z")
            }
        }.forEach {
            XposedBridge.hookMethod(it.getMethodInstance(classLoader), spoofCN)
        }

    }
}
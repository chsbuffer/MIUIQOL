package io.github.chsbuffer.miuihelper.hooks.updater

import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import io.github.chsbuffer.miuihelper.model.Hook
import io.luckypray.dexkit.DexKitBridge
import io.luckypray.dexkit.enums.MatchType

class RemoveOTAValidate(val dexKit: DexKitBridge) : Hook() {

    override fun init() {
        if (!xPrefs.getBoolean("remove_ota_validate", false))
            return

        val m = dexKit.findMethodUsingString {
            usingString = "support_ota_validate"
            matchType = MatchType.FULL
        }.single()
            .getMethodInstance(classLoader)

        XposedBridge.hookMethod(m, XC_MethodReplacement.returnConstant(false))

        /*
        return FeatureParser.getBoolean("support_ota_validate", false);
        */
    }
}
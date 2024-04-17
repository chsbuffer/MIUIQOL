package io.github.chsbuffer.miuihelper.hooks.updater

import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import io.github.chsbuffer.miuihelper.model.Hook
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.query.enums.StringMatchType

class RemoveOTAValidate(val dexKit: DexKitBridge) : Hook() {

    override fun init() {
        if (!xPrefs.getBoolean("remove_ota_validate", false))
            return

        val m = dexKit.findMethod {
            matcher {
                addUsingString("support_ota_validate", StringMatchType.Equals)
            }
            findFirst = true
        }.single().getMethodInstance(classLoader)

        XposedBridge.hookMethod(m, XC_MethodReplacement.returnConstant(false))

        /*
        return FeatureParser.getBoolean("support_ota_validate", false);
        */
    }
}
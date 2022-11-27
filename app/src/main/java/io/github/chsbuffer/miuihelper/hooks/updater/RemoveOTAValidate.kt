package io.github.chsbuffer.miuihelper.hooks.updater

import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import io.github.chsbuffer.miuihelper.hooks.updater.UpdaterHost.dexKit
import io.github.chsbuffer.miuihelper.model.Hook

object RemoveOTAValidate : Hook() {
    override fun init(classLoader: ClassLoader) {
        if (!xPrefs.getBoolean("remove_ota_validate", false))
            return

        val m = dexKit.findMethodUsingString("support_ota_validate").single()
            .getMethodInstance(classLoader)

        XposedBridge.hookMethod(m, XC_MethodReplacement.returnConstant(false))

        /*
        return FeatureParser.getBoolean("support_ota_validate", false);
        */
    }
}
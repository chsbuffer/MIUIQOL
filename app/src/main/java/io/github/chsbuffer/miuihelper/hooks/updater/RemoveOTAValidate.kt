package io.github.chsbuffer.miuihelper.hooks.updater

import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedHelpers
import io.github.chsbuffer.miuihelper.model.Hook

object RemoveOTAValidate : Hook() {
    override fun init(classLoader: ClassLoader) {
        if (!xPrefs.getBoolean("remove_ota_validate", false))
            return

        val (clazzName, methodName) = UpdaterHost.version.let {
            when {
                else -> Pair("i", "T")
            }
        }

        XposedHelpers.findAndHookMethod(
            "com.android.updater.common.utils.$clazzName",
            classLoader,
            methodName,
            XC_MethodReplacement.returnConstant(false)
        )
        /*
        return FeatureParser.getBoolean("support_ota_validate", false);
        */
    }
}
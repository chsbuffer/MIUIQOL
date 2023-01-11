package io.github.chsbuffer.miuihelper.util

import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import io.github.chsbuffer.miuihelper.model.Hook

object XposedUtil {
    @JvmStatic
    fun hooks(lpparam: LoadPackageParam, vararg hooks: Hook) {
        for (hook in hooks) {
            try {
                hook.init(lpparam)
            } catch (e: Exception) {
                XposedBridge.log(
                    "Failed to do ${hook::class.java.simpleName} hook\n${e}"
                )
            } catch (e: Error) {
                XposedBridge.log(
                    "Failed to do ${hook::class.java.simpleName} hook\n${e}"
                )
            }
        }
    }
}
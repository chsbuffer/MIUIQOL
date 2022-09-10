package io.github.chsbuffer.miuihelper.util

import de.robv.android.xposed.XposedBridge
import io.github.chsbuffer.miuihelper.model.Hook

object XposedUtil {
    @JvmStatic
    fun hooks(classLoader: ClassLoader, vararg hooks: Hook) {
        for (hook in hooks) {
            try {
                hook.init(classLoader)
            } catch (e: Exception) {
                XposedBridge.log(
                    "Failed to do ${hook::class.java.simpleName} hook\n${e.message}"
                )
            } catch (e: Error) {
                XposedBridge.log(
                    "Failed to do ${hook::class.java.simpleName} hook\n${e.message}"
                )
            }
        }
    }
}
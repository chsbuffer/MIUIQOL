package io.github.chsbuffer.miuihelper.hooks.screenshot

import de.robv.android.xposed.XposedHelpers
import io.github.chsbuffer.miuihelper.model.Hook

object SaveToPictures: Hook() {
    override fun init() {
        if (!xPrefs.getBoolean("save_to_pictures", true))
            return
        val clazz = XposedHelpers.findClass("android.os.Environment", classLoader)
        XposedHelpers.setStaticObjectField(clazz, "DIRECTORY_DCIM", "Pictures")
    }
}
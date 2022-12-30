package io.github.chsbuffer.miuihelper.hooks.screenrecorder

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import io.github.chsbuffer.miuihelper.model.Hook

object SaveToMovies : Hook() {
    @Suppress("UNCHECKED_CAST")
    override fun init(classLoader: ClassLoader) {
        if (!xPrefs.getBoolean("save_to_movies", true)) return

        /**/
        val clazz = XposedHelpers.findClass("android.os.Environment", classLoader)
        XposedHelpers.setStaticObjectField(clazz, "DIRECTORY_DCIM", "Movies")

        /**/
        XposedHelpers.findAndHookMethod("android.content.ContentValues",
            classLoader,
            "put",
            String::class.java,
            String::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (param.args[0] == "relative_path") {
                        param.args[1] = (param.args[1] as String).replace("DCIM", "Movies")
                    }
                }
            })
    }
}
package io.github.chsbuffer.miuihelper.hooks.gallery

import android.os.Environment
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedHelpers
import io.github.chsbuffer.miuihelper.model.Hook


object FixAlbum : Hook() {
    override fun init(classLoader: ClassLoader) {
        XposedHelpers.findAndHookMethod(
            "com.miui.gallery.model.dto.Album",
            classLoader,
            "isMustVisibleAlbum",
            String::class.java,
            XC_MethodReplacement.returnConstant(true)
        )

        val saveToPictures = xPrefs.getBoolean("save_to_pictures", true)
        val saveToMovies = xPrefs.getBoolean("save_to_movies", true)
        val constantClazz =
            XposedHelpers.findClass(
                "com.miui.gallery.storage.constants.MIUIStorageConstants",
                classLoader
            )
        if (saveToPictures)
            XposedHelpers.setStaticObjectField(
                constantClazz, "DIRECTORY_SCREENSHOT_PATH",
                Environment.DIRECTORY_PICTURES + "/Screenshots"
            )

        if (saveToMovies)
            XposedHelpers.setStaticObjectField(
                constantClazz, "DIRECTORY_SCREENRECORDER_PATH",
                Environment.DIRECTORY_MOVIES + "/screenrecorder"
            )
    }
}
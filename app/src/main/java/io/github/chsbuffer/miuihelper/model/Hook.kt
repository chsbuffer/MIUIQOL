package io.github.chsbuffer.miuihelper.model

import de.robv.android.xposed.XSharedPreferences
import io.github.chsbuffer.miuihelper.BuildConfig

abstract class Hook {
    companion object {
        val xPrefs = XSharedPreferences(
            BuildConfig.APPLICATION_ID,
            "prefs"
        )
    }

    abstract fun init(classLoader: ClassLoader)
}
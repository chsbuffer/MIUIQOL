package io.github.chsbuffer.miuihelper.model

import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import io.github.chsbuffer.miuihelper.BuildConfig

abstract class Hook {
    companion object {
        val xPrefs = XSharedPreferences(
            BuildConfig.APPLICATION_ID,
            "prefs"
        )
    }

    open fun init(lpparam: LoadPackageParam) {
        init(lpparam.classLoader)
    }

    open fun init(classLoader: ClassLoader) {

    }
}
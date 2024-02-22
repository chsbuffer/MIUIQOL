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

    protected lateinit var lpparam: LoadPackageParam
    protected lateinit var classLoader: ClassLoader

    fun init(lpparam: LoadPackageParam) {
        this.lpparam = lpparam
        this.classLoader = lpparam.classLoader

        init()
    }

    open fun init() {

    }
}
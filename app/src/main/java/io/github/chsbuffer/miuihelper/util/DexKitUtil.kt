package io.github.chsbuffer.miuihelper.util

import android.app.Application
import io.luckypray.dexkit.DexKitBridge

class DexKitUtil {
    companion object {
        init {
            System.loadLibrary("dexkit")
        }

        @JvmStatic
        fun get(application: Application): DexKitBridge? {
            return DexKitBridge.create(application.applicationInfo.sourceDir)
        }
    }
}
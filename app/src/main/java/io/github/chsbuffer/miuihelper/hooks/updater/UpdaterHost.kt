package io.github.chsbuffer.miuihelper.hooks.updater

import de.robv.android.xposed.callbacks.XC_LoadPackage
import io.github.chsbuffer.miuihelper.model.Hook
import io.github.chsbuffer.miuihelper.util.hooks
import io.github.chsbuffer.miuihelper.util.useDexKit

object UpdaterHost : Hook() {

    override fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        useDexKit(lpparam) { dexKit ->
            hooks(
                lpparam,
                RemoveOTAValidate(dexKit)
            )
        }
    }
}
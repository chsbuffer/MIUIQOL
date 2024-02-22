package io.github.chsbuffer.miuihelper.hooks.systemui

import de.robv.android.xposed.XposedHelpers
import io.github.chsbuffer.miuihelper.model.BooleanDuringMethod
import io.github.chsbuffer.miuihelper.model.Hook
import miui.os.Build


object RestoreCnQuickAccessWalletTile : Hook() {
    override fun init() {
        if (!xPrefs.getBoolean("restore_wallet_tile", true)) return

        if (Build.IS_INTERNATIONAL_BUILD) return

        XposedHelpers.findAndHookMethod(
            "com.android.systemui.qs.tiles.QuickAccessWalletTile",
            classLoader,
            "isAvailable",
            BooleanDuringMethod(
                XposedHelpers.findClass(
                    "miui.os.Build", classLoader
                ), "IS_INTERNATIONAL_BUILD", true
            )
        )
    }
}
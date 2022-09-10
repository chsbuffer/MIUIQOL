package io.github.chsbuffer.miuihelper.hooks.securitycenter

import android.view.View
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedHelpers
import io.github.chsbuffer.miuihelper.model.Hook


object LockOneHundred : Hook() {
    override fun init(classLoader: ClassLoader) {
        if (!xPrefs.getBoolean("lock_one_hundred", false))
            return
        // 从 WooBox for MIUI 复制的
        //锁定100分
        //防止点击重新检测
        XposedHelpers.findAndHookMethod(
            "com.miui.securityscan.ui.main.MainContentFrame", classLoader,
            "onClick",
            View::class.java,
            XC_MethodReplacement.returnConstant(null)
        )
        XposedHelpers.findAndHookMethod(
            "com.miui.securityscan.scanner.ScoreManager", classLoader,
            "B",
            XC_MethodReplacement.returnConstant(0)
        )
    }
}
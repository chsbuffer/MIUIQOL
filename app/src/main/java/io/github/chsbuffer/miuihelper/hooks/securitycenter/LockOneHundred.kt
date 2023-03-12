package io.github.chsbuffer.miuihelper.hooks.securitycenter

import android.view.View
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import io.github.chsbuffer.miuihelper.model.Hook
import io.luckypray.dexkit.DexKitBridge
import io.luckypray.dexkit.enums.MatchType


class LockOneHundred(val dexKit: DexKitBridge) : Hook() {
    override fun init(classLoader: ClassLoader) {
        if (!xPrefs.getBoolean("lock_one_hundred", false))
            return
        //防止点击重新检测
        XposedHelpers.findAndHookMethod(
            "com.miui.securityscan.ui.main.MainContentFrame", classLoader,
            "onClick",
            View::class.java,
            XC_MethodReplacement.returnConstant(null)
        )
        //锁定100分
        val minusScoreMethod = dexKit.findMethodUsingString {
            usingString = "getMinusPredictScore"
            matchType = MatchType.CONTAINS
            methodDeclareClass = "com.miui.securityscan.scanner.ScoreManager"
        }.single()
/*
    public int p() {
        int n10 = n();
        if (n10 > 100) {
            n10 = 100;
        } else if (n10 < 0) {
            n10 = 0;
        }
        return 100 - n10;
    }
    private int n() {
        if (f16651p) {
            Log.d("ScoreManager", "getMinusPredictScore------------------------------------------------ ");
        }
...
 */
        XposedBridge.hookMethod(
            minusScoreMethod.getMethodInstance(classLoader),
            XC_MethodReplacement.returnConstant(0)
        )
    }
}
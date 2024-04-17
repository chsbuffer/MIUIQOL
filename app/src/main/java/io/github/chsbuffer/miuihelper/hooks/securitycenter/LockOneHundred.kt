package io.github.chsbuffer.miuihelper.hooks.securitycenter

import android.view.View
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import io.github.chsbuffer.miuihelper.model.Hook
import io.github.chsbuffer.miuihelper.util.logSearch
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.query.enums.StringMatchType


class LockOneHundred(val dexKit: DexKitBridge) : Hook() {
    override fun init() {
        if (!xPrefs.getBoolean("lock_one_hundred", true)) return
        //防止点击重新检测
        XposedHelpers.findAndHookMethod(
            "com.miui.securityscan.ui.main.MainContentFrame",
            classLoader,
            "onClick",
            View::class.java,
            XC_MethodReplacement.returnConstant(null)
        )
        //锁定100分
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
        val minusScoreMethod = logSearch("securityScan_getMinusPredictScore", { it.descriptor }) {
            dexKit.getClassData("com.miui.securityscan.scanner.ScoreManager")!!.findMethod {
                matcher {
                    addUsingString("getMinusPredictScore", StringMatchType.Contains)
                }
                findFirst = true
            }.single()
        }

        XposedBridge.hookMethod(
            minusScoreMethod.getMethodInstance(classLoader), XC_MethodReplacement.returnConstant(0)
        )
    }
}
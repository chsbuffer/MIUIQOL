package io.github.chsbuffer.miuihelper

import androidx.annotation.Keep
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import io.github.chsbuffer.miuihelper.hooks.aiasst.SupportAiSubtitles
import io.github.chsbuffer.miuihelper.hooks.home.RestoreCnBuildGoogleApp
import io.github.chsbuffer.miuihelper.hooks.home.RestoreGoogleSearch
import io.github.chsbuffer.miuihelper.hooks.home.RestoreSwitchMinusScreen
import io.github.chsbuffer.miuihelper.hooks.screenrecorder.ForceSupportPlaybackCapture
import io.github.chsbuffer.miuihelper.hooks.screenrecorder.SaveToMovies
import io.github.chsbuffer.miuihelper.hooks.screenshot.SaveToPictures
import io.github.chsbuffer.miuihelper.hooks.securitycenter.AppDetailsStockOpenDefaultSettings
import io.github.chsbuffer.miuihelper.hooks.securitycenter.AppDetailsSystemAppWlanControl
import io.github.chsbuffer.miuihelper.hooks.securitycenter.DexKitCache
import io.github.chsbuffer.miuihelper.hooks.securitycenter.EnabledAllTextView
import io.github.chsbuffer.miuihelper.hooks.securitycenter.IntlEnableBehaviorRecord
import io.github.chsbuffer.miuihelper.hooks.securitycenter.LockOneHundred
import io.github.chsbuffer.miuihelper.hooks.securitycenter.RemoveBehaviorRecordWhiteListAndNoIgnoreSystemApp
import io.github.chsbuffer.miuihelper.hooks.securitycenter.RemoveSetSystemAppWifiRuleAllow
import io.github.chsbuffer.miuihelper.hooks.systemui.NotificationClickInfoItemStartChannelSetting
import io.github.chsbuffer.miuihelper.hooks.systemui.NotificationSettingsNoWhiteList
import io.github.chsbuffer.miuihelper.hooks.systemui.RestoreCnNearby
import io.github.chsbuffer.miuihelper.hooks.systemui.RestoreCnQuickAccessWalletTile
import io.github.chsbuffer.miuihelper.hooks.updater.RemoveOTAValidate
import io.github.chsbuffer.miuihelper.util.hooks
import io.github.chsbuffer.miuihelper.util.inContext
import io.github.chsbuffer.miuihelper.util.useDexKit

@Keep
class MainHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        when (lpparam.packageName) {
            "com.android.updater" -> useDexKit(lpparam) { dexKit ->
                hooks(
                    lpparam,
                    RemoveOTAValidate(dexKit)
                )
            }

            "com.miui.securitycenter" -> inContext(lpparam) { app ->
                useDexKit(lpparam) { dexKit ->
                    val dexKitCache = DexKitCache()
                    hooks(
                        lpparam,
                        RemoveBehaviorRecordWhiteListAndNoIgnoreSystemApp(dexKitCache),
                        RemoveSetSystemAppWifiRuleAllow,
                        EnabledAllTextView,
                        LockOneHundred(dexKit),
                        AppDetailsSystemAppWlanControl(dexKitCache, app),
                        AppDetailsStockOpenDefaultSettings(dexKitCache, app),
                        IntlEnableBehaviorRecord(dexKitCache)
                    )
                }
            }

            "com.miui.screenrecorder" -> hooks(
                lpparam,
                SaveToMovies,
                ForceSupportPlaybackCapture
            )

            "com.miui.screenshot" -> hooks(
                lpparam,
                SaveToPictures
            )

            "com.miui.home" -> inContext(lpparam) {
                hooks(
                    lpparam,
                    RestoreCnBuildGoogleApp,
                    RestoreSwitchMinusScreen,
                    RestoreGoogleSearch
                )
            }

            "com.xiaomi.aiasst.vision" -> hooks(
                lpparam,
                SupportAiSubtitles
            )

            "com.android.systemui" -> useDexKit(lpparam) { dexKit ->
                hooks(
                    lpparam,
                    RestoreCnNearby(dexKit),
                    RestoreCnQuickAccessWalletTile,
                    NotificationSettingsNoWhiteList,
                    NotificationClickInfoItemStartChannelSetting
                )
            }
        }
    }
}
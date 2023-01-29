package io.github.chsbuffer.miuihelper

import androidx.annotation.Keep
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import io.github.chsbuffer.miuihelper.hooks.aiasst.SupportAiSubtitles
import io.github.chsbuffer.miuihelper.hooks.home.RestoreCnBuildGoogleApp
import io.github.chsbuffer.miuihelper.hooks.screenrecorder.ForceSupportPlaybackCapture
import io.github.chsbuffer.miuihelper.hooks.screenrecorder.SaveToMovies
import io.github.chsbuffer.miuihelper.hooks.screenshot.SaveToPictures
import io.github.chsbuffer.miuihelper.hooks.securitycenter.SecurityHost
import io.github.chsbuffer.miuihelper.hooks.systemui.NotificationClickInfoItemStartChannelSetting
import io.github.chsbuffer.miuihelper.hooks.systemui.NotificationSettingsNoWhiteList
import io.github.chsbuffer.miuihelper.hooks.systemui.RestoreCnNearby
import io.github.chsbuffer.miuihelper.hooks.updater.UpdaterHost
import io.github.chsbuffer.miuihelper.util.hooks

@Keep
class MainHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        when (lpparam.packageName) {
            "com.android.updater" -> hooks(
                lpparam,
                UpdaterHost
            )
            "com.miui.securitycenter" -> hooks(
                lpparam,
                SecurityHost
            )
            "com.miui.screenrecorder" -> hooks(
                lpparam,
                SaveToMovies,
                ForceSupportPlaybackCapture
            )
            "com.miui.screenshot" -> hooks(
                lpparam,
                SaveToPictures
            )
            "com.miui.home" -> hooks(
                lpparam,
                RestoreCnBuildGoogleApp
            )
            "com.xiaomi.aiasst.vision" -> hooks(
                lpparam,
                SupportAiSubtitles
            )
            "com.android.systemui" -> hooks(
                lpparam,
                RestoreCnNearby,
                NotificationSettingsNoWhiteList,
                NotificationClickInfoItemStartChannelSetting
            )
        }
    }
}
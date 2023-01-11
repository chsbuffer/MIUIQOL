package io.github.chsbuffer.miuihelper;

import androidx.annotation.Keep;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.chsbuffer.miuihelper.hooks.aiasst.SupportAiSubtitles;
import io.github.chsbuffer.miuihelper.hooks.home.RestoreCnBuildGoogleApp;
import io.github.chsbuffer.miuihelper.hooks.screenrecorder.ForceSupportPlaybackCapture;
import io.github.chsbuffer.miuihelper.hooks.screenrecorder.SaveToMovies;
import io.github.chsbuffer.miuihelper.hooks.screenshot.SaveToPictures;
import io.github.chsbuffer.miuihelper.hooks.securitycenter.SecurityHost;
import io.github.chsbuffer.miuihelper.hooks.systemui.NotificationSettingsNoWhiteList;
import io.github.chsbuffer.miuihelper.hooks.systemui.RestoreCnNearby;
import io.github.chsbuffer.miuihelper.hooks.updater.UpdaterHost;
import io.github.chsbuffer.miuihelper.util.XposedUtil;

@Keep
public class MainHook implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        switch (lpparam.packageName) {
            case "com.android.updater":
                XposedUtil.hooks(lpparam,
                        UpdaterHost.INSTANCE
                );
                break;
            case "com.miui.securitycenter":
                XposedUtil.hooks(lpparam,
                        SecurityHost.INSTANCE
                );
                break;
            case "com.miui.screenrecorder":
                XposedUtil.hooks(lpparam,
                        SaveToMovies.INSTANCE,
                        ForceSupportPlaybackCapture.INSTANCE
                );
                break;
            case "com.miui.screenshot":
                XposedUtil.hooks(lpparam,
                        SaveToPictures.INSTANCE);
                break;
            case "com.miui.home":
                XposedUtil.hooks(lpparam,
                        RestoreCnBuildGoogleApp.INSTANCE
                );
                break;
            case "com.xiaomi.aiasst.vision":
                XposedUtil.hooks(lpparam,
                        SupportAiSubtitles.INSTANCE
                );
                break;
            case "com.android.systemui":
                XposedUtil.hooks(lpparam,
                        RestoreCnNearby.INSTANCE,
                        NotificationSettingsNoWhiteList.INSTANCE
                );
                break;
        }
    }
}
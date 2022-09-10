package io.github.chsbuffer.miuihelper;

import androidx.annotation.Keep;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.chsbuffer.miuihelper.hooks.aiasst.SupportAiSubtitles;
import io.github.chsbuffer.miuihelper.hooks.home.RestoreCnBuildGoogleApp;
import io.github.chsbuffer.miuihelper.hooks.msystemui.RestoreCnNearby;
import io.github.chsbuffer.miuihelper.hooks.screenrecorder.SaveToMovies;
import io.github.chsbuffer.miuihelper.hooks.screenshot.SaveToPictures;
import io.github.chsbuffer.miuihelper.hooks.securitycenter.Startup;
import io.github.chsbuffer.miuihelper.hooks.updater.UpdaterStartup;
import io.github.chsbuffer.miuihelper.util.XposedUtil;

@Keep
public class MainHook implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        switch (lpparam.packageName) {
            case "com.android.updater":
                XposedUtil.hooks(lpparam.classLoader,
                        UpdaterStartup.INSTANCE
                );
                break;
            case "com.miui.securitycenter":
                XposedUtil.hooks(lpparam.classLoader,
                        Startup.INSTANCE
                );
                break;
            case "com.miui.screenrecorder":
                XposedUtil.hooks(lpparam.classLoader,
                        SaveToMovies.INSTANCE);
                break;
            case "com.miui.screenshot":
                XposedUtil.hooks(lpparam.classLoader,
                        SaveToPictures.INSTANCE);
                break;
            case "com.miui.home":
                XposedUtil.hooks(lpparam.classLoader,
                        RestoreCnBuildGoogleApp.INSTANCE
                );
                break;
            case "com.xiaomi.aiasst.vision":
                XposedUtil.hooks(lpparam.classLoader,
                        SupportAiSubtitles.INSTANCE
                );
                break;
            case "com.android.systemui":
                XposedUtil.hooks(lpparam.classLoader,
                        RestoreCnNearby.INSTANCE
                );
                break;
        }
    }
}
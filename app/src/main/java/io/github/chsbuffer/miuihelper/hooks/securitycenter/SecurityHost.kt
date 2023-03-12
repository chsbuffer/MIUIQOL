package io.github.chsbuffer.miuihelper.hooks.securitycenter

import de.robv.android.xposed.callbacks.XC_LoadPackage
import io.github.chsbuffer.miuihelper.model.Hook
import io.github.chsbuffer.miuihelper.util.hooks
import io.github.chsbuffer.miuihelper.util.inContext
import io.github.chsbuffer.miuihelper.util.useDexKit


object SecurityHost : Hook() {
    override fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        inContext(lpparam) { app ->
            useDexKit(lpparam) { dexKit ->
                hooks(
                    lpparam,
                    RemoveBehaviorRecordWhiteListAndNoIgnoreSystemApp(dexKit),
                    RemoveSetSystemAppWifiRuleAllow,
                    EnabledAllTextView,
                    LockOneHundred(dexKit),
                    AppDetails(dexKit, app),
                    IntlEnableBehaviorRecord(dexKit)
                )
            }
        }
    }
}
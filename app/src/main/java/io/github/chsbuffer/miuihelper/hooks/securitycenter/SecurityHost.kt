package io.github.chsbuffer.miuihelper.hooks.securitycenter

import io.github.chsbuffer.miuihelper.model.Hook
import io.github.chsbuffer.miuihelper.model.Host


object SecurityHost : Host() {
    override var hooks: Array<Hook> = arrayOf(
        RemoveBehaviorRecordWhiteListAndNoIgnoreSystemApp,
        RemoveSetSystemAppWifiRuleAllow,
        EnabledAllTextView,
        LockOneHundred,
        AppDetails,
        IntlEnableBehaviorRecord
    )
}
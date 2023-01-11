package io.github.chsbuffer.miuihelper.hooks.updater

import io.github.chsbuffer.miuihelper.model.Hook
import io.github.chsbuffer.miuihelper.model.Host

object UpdaterHost : Host() {

    override var hooks: Array<Hook> = arrayOf(RemoveOTAValidate)

}
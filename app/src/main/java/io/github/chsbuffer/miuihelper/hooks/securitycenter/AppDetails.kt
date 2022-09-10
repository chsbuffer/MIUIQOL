package io.github.chsbuffer.miuihelper.hooks.securitycenter

import android.content.DialogInterface
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import io.github.chsbuffer.miuihelper.model.BooleanDuringMethod
import io.github.chsbuffer.miuihelper.model.Hook


object AppDetails : Hook() {
    override fun init(classLoader: ClassLoader) {
        if (!xPrefs.getBoolean("system_app_wlan_control", true))
            return

        val clazz =
            XposedHelpers.findClass("com.miui.appmanager.ApplicationsDetailsActivity", classLoader)

        XposedHelpers.findAndHookMethod(
            clazz,
            "initView",
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun beforeHookedMethod(param: MethodHookParam) {

                    val (isSystemAppField, summaryMethod, onClickMethod) = Startup.version.let {
                        when {
                            it >= 40000711 -> Triple("o0", "L", "b0")
                            it >= 40000703 -> Triple("o0", "J", "Z")
                            it >= 40000620 -> Triple("n0", "I", "Z")
                            it >= 0 -> Triple("n0", "M", "b0")
                            else -> throw Exception()
                        }
                    }

                    val hook = BooleanDuringMethod(param.thisObject, isSystemAppField, false)

                    clazz.declaredClasses.first {
                        if (DialogInterface.OnMultiChoiceClickListener::class.java
                                .isAssignableFrom(it)
                        ) {
                            val method = it.methods.firstOrNull { m -> m.name == "onClick" }
//                            b0()
//                                this.v0 = this.q0;
//                                this.t0 = this.r0;
//                                this.u0 = this.s0;
//
//                            ApplicationDetails.u(OnMultiChoiceClickListener).onClick(DialogInterface,int,boolean)
//                                if (applicationsDetailsActivity.o0) {
//                                    if (i != 0) {
//                                        if (i != 1) {
//                                            return;
//                                        }
//                                        applicationsDetailsActivity.u0 = z;
//                                        return;
//                                    }
//                                    applicationsDetailsActivity.t0 = z;
//                                }
                            if (method != null) {
                                XposedBridge.hookMethod(method, hook)
                                return@first true
                            }
                        }
                        false
                    }
                        ?: throw Exception("OnMultiChoiceClickListener not found. wlan setting for System apps will not be available in app details")

                    XposedHelpers.findAndHookMethod(clazz, summaryMethod, hook)
//                    this.m.setSummary(L());
//                    ...
//                    L() {
//                        if (this.o0) {
//                            i2 = C0412R.string.app_manager_system_mobile_disable;
//                        } else if (!this.q0) {
//                            i2 = C0412R.string.app_manager_disable;
//                        }

                    XposedHelpers.findAndHookMethod(clazz, onClickMethod, hook)
//                b0();
//                str = "network_control";

                }

            })
    }
}
package io.github.chsbuffer.miuihelper.model

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

class BooleanDuringMethod : XC_MethodHook {

    var clazz: Class<*>? = null
    var obj: Any? = null
    val fieldName: String
    val value: Boolean

    constructor(fieldName: String, value: Boolean) : super() {
        this.fieldName = fieldName
        this.value = value
    }

    constructor(obj: Any, fieldName: String, value: Boolean) : super() {
        this.obj = obj
        this.fieldName = fieldName
        this.value = value
    }

    constructor(clazz: Class<*>, fieldName: String, value: Boolean) : super() {
        this.clazz = clazz
        this.fieldName = fieldName
        this.value = value
    }

    private var oldValue: Boolean = false

    override fun beforeHookedMethod(param: MethodHookParam) {
        if (obj != null || (obj == null && clazz == null)) {
            if (obj == null) obj = param.thisObject

            oldValue = XposedHelpers.getBooleanField(obj, fieldName)
            XposedHelpers.setBooleanField(obj, fieldName, value)
        } else if (clazz != null) {
            oldValue = XposedHelpers.getStaticBooleanField(clazz, fieldName)
            XposedHelpers.setStaticBooleanField(clazz, fieldName, value)
        }
    }

    override fun afterHookedMethod(param: MethodHookParam?) {
        if (obj != null) {
            XposedHelpers.setBooleanField(obj, fieldName, oldValue)
        } else {
            XposedHelpers.setStaticBooleanField(clazz, fieldName, oldValue)
        }
    }
}
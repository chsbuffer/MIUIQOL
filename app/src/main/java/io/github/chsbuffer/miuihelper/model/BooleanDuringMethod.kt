package io.github.chsbuffer.miuihelper.model

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

class BooleanDuringMethod : XC_MethodHook {
    enum class Type {
        ThisObj, Obj, Class, Func
    }

    private var type: Type
    private var clazz: Class<*>? = null
    private var obj: Any? = null
    private var func: ((MethodHookParam) -> Any)? = null

    private val fieldName: String
    private val value: Boolean

    constructor(fieldName: String, value: Boolean) : super() {
        type = Type.ThisObj
        this.fieldName = fieldName
        this.value = value
    }

    constructor(func: (MethodHookParam) -> Any, fieldName: String, value: Boolean) : super() {
        type = Type.Func
        this.func = func
        this.fieldName = fieldName
        this.value = value
    }

    constructor(obj: Any, fieldName: String, value: Boolean) : super() {
        type = Type.Obj
        this.obj = obj
        this.fieldName = fieldName
        this.value = value
    }

    constructor(clazz: Class<*>, fieldName: String, value: Boolean) : super() {
        type = Type.Class
        this.clazz = clazz
        this.fieldName = fieldName
        this.value = value
    }

    private var oldValue: Boolean = false

    override fun beforeHookedMethod(param: MethodHookParam) {
        when (type) {
            Type.ThisObj -> obj = param.thisObject
            Type.Func -> obj = func!!(param)
            Type.Class -> {
                oldValue = XposedHelpers.getStaticBooleanField(clazz, fieldName)
                XposedHelpers.setStaticBooleanField(clazz, fieldName, value)
                return
            }
            else -> {}
        }

        oldValue = XposedHelpers.getBooleanField(obj, fieldName)
        XposedHelpers.setBooleanField(obj, fieldName, value)
    }

    override fun afterHookedMethod(param: MethodHookParam?) {
        when (type) {
            Type.ThisObj, Type.Obj, Type.Func -> XposedHelpers.setBooleanField(
                obj,
                fieldName,
                oldValue
            )
            Type.Class -> XposedHelpers.setStaticBooleanField(clazz, fieldName, oldValue)
        }
    }
}
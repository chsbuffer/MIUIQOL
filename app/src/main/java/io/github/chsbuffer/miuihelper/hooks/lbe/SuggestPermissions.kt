package io.github.chsbuffer.miuihelper.hooks.lbe

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import io.github.chsbuffer.miuihelper.model.Hook

// DO_NOT_USE_OR_YOU_WILL_BE_FIRED
object SuggestPermissions : Hook() {

    private const val PERM_ID_AUTOSTART: Long = 16384

    private const val PERM_ID_INSTALL_SHORTCUT = 4503599627370496L

    private const val DefaultAccept = PERM_ID_AUTOSTART or PERM_ID_INSTALL_SHORTCUT

    override fun init() {
        val clazz =
            XposedHelpers.findClass("com.lbe.security.bean.AppPermissionConfig", classLoader)

        XposedHelpers.findAndHookMethod(clazz,
            "setSuggestAccept",
            Long::class.javaPrimitiveType,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.args[0] = (param.args[0] as Long) or DefaultAccept
                }
            })

        XposedHelpers.findAndHookMethod(clazz,
            "setSuggestReject",
            Long::class.javaPrimitiveType,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.args[0] = (param.args[0] as Long) xor DefaultAccept
                }
            })

        XposedHelpers.findAndHookMethod(clazz,
            "setSuggestedPermissions",
            Long::class.javaPrimitiveType,
            Long::class.javaPrimitiveType,
            Long::class.javaPrimitiveType,
            Long::class.javaPrimitiveType,
            Long::class.javaPrimitiveType,
            Long::class.javaPrimitiveType,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.args[0] = (param.args[0] as Long) or DefaultAccept
                    param.args[3] = (param.args[3] as Long) xor DefaultAccept
                }
            })
    }
}
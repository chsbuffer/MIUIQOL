package io.github.chsbuffer.miuihelper.hooks.screenrecorder

import android.app.AndroidAppHelper
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.media.MediaMuxer
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedHelpers
import io.github.chsbuffer.miuihelper.model.Hook

object SaveToMovies : Hook() {
    @Suppress("UNCHECKED_CAST")
    override fun init(classLoader: ClassLoader) {
        if (!xPrefs.getBoolean("save_to_movies", true))
            return

        // let record store at Movies instead of DCIM
        val clazz = XposedHelpers.findClass("android.os.Environment", classLoader)
        XposedHelpers.setStaticObjectField(clazz, "DIRECTORY_DCIM", "Movies")

        XposedHelpers.findAndHookMethod(
            "com.miui.screenrecorder.service.BaseRecorderService$4",
            classLoader,
            "onFolderBtnClick",
            object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam): Any? {
                    val intent = Intent()
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    intent.setClassName(
                        "com.miui.gallery",
                        "com.miui.gallery.activity.HomePageActivity"
                    )
                    AndroidAppHelper.currentApplication().applicationContext.startActivity(intent)
                    return null
                }
            })
        XposedHelpers.findAndHookMethod(
            "com.miui.screenrecorder.StableScreenRecorder",
            classLoader,
            "prepareMediaMuxerByMediaStore",
            String::class.java,
            object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam): Any? {
                    val TAG = "StableScreenRecorderCore"
                    val obj = param.thisObject
                    val str = param.args[0] as String
                    val contentResolver = AndroidAppHelper.currentApplication().contentResolver
                    val contentValues = ContentValues()
                    contentValues.put("relative_path", "Movies/ScreenRecorder")
                    contentValues.put(
                        "_display_name",
                        str.substring(str.lastIndexOf('/') + 1)
                    )
                    contentValues.put("is_pending", 1)
                    val insert = contentResolver.insert(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                    )
                    XposedHelpers.setObjectField(obj, "mRecordingUri", insert)
                    XposedHelpers.setObjectField(obj, "mParcelFileDescriptor", null)
                    return try {
                        XposedHelpers.setObjectField(
                            obj, "mParcelFileDescriptor", contentResolver.openFileDescriptor(
                                insert!!, "rw"
                            )
                        )
                        MediaMuxer(
                            (XposedHelpers.getObjectField(
                                obj,
                                "mParcelFileDescriptor"
                            ) as ParcelFileDescriptor).fileDescriptor,
                            MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
                        )
                    } catch (e3: Exception) {
                        Log.e(TAG, "prepareMediaMuxerByMediaStore Exception: $str,$e3")
                        null
                    }
                }
            })
        XposedHelpers.findAndHookMethod(
            "com.miui.screenrecorder.tools.Utils",
            classLoader,
            "screenRecorderFileExist",
            object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam): Any {
                    // idk what this method does
                    val TAG = "Utils"
                    var result = false
                    var cursor: Cursor? = null
                    return try {
                        try {
                            cursor =
                                AndroidAppHelper.currentApplication().applicationContext.contentResolver.query(
                                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                    arrayOf("_id"),
                                    "is_pending=0 AND _data LIKE '%/Movies/ScreenRecorder/%' ",
                                    null,
                                    null
                                )
                            val count = cursor?.count ?: 0
                            Log.v(TAG, "screenrecorder videos count: $count")
                            if (count > 0) {
                                result = true
                            }
                            cursor?.close()
                            result
                        } catch (err: Exception) {
                            Log.e(TAG, "get screenrecorder video count failed, err:", err)
                            cursor?.close()
                            false
                        }
                    } catch (th: Throwable) {
                        cursor?.close()
                        throw th
                    }
                }
            })

    }
}
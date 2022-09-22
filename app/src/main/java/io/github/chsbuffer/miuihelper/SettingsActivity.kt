@file:Suppress("DEPRECATION")
@file:SuppressLint("WorldReadableFiles")

package io.github.chsbuffer.miuihelper

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.preference.Preference
import android.preference.Preference.OnPreferenceChangeListener
import android.preference.Preference.OnPreferenceClickListener
import android.preference.PreferenceFragment
import android.preference.SwitchPreference
import kotlin.system.exitProcess


class SettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragmentManager.beginTransaction()
            .replace(android.R.id.content, SettingsFragment())
            .commit()
    }

    class SettingsFragment : PreferenceFragment(), OnPreferenceChangeListener,
        OnPreferenceClickListener {

        @Deprecated("Deprecated in Java")
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.header_preferences)

            val isModuleActivated: Boolean = try {
                context.getSharedPreferences("prefs", MODE_WORLD_READABLE)
                true
            } catch (e: SecurityException) {
                false
            }

            if (!isModuleActivated) {
                addPreferencesFromResource(R.xml.warn_preference)
                findPreference("warn").onPreferenceClickListener = this
                return
            }

            preferenceManager.sharedPreferencesMode = MODE_WORLD_READABLE
            preferenceManager.sharedPreferencesName = "prefs"
            addPreferencesFromResource(R.xml.root_preferences)

            findPreference("behavior_record_enhance").onPreferenceChangeListener = this
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                val originalDefaultOpenSettingPreference = findPreference("original_default_open_setting")
                originalDefaultOpenSettingPreference.parent!!.removePreference(
                    originalDefaultOpenSettingPreference
                )
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean =
            when (preference.key) {
                "behavior_record_enhance" -> {
                    (findPreference("behavior_record_system_app_whitelist") as SwitchPreference)
                        .isChecked = newValue as Boolean
                    true
                }
                else -> true
            }

        @Deprecated("Deprecated in Java")
        override fun onPreferenceClick(preference: Preference) = when (preference.key) {
            "warn" -> {
                exitProcess(0)
            }
            else -> false
        }
    }
}
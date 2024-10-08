// Top-level build file where you can add configuration options common to all sub-projects/modules.

import com.android.build.api.dsl.CommonExtension
import com.android.build.gradle.api.AndroidBasePlugin

val sdk by extra(35)

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.refine) apply false
}

subprojects {
    plugins.withType(AndroidBasePlugin::class.java) {
        extensions.configure(CommonExtension::class.java) {
            compileSdk = sdk

            defaultConfig {
                minSdk = 27
            }

            packaging.resources {
                excludes += arrayOf(
                    "META-INF/**",
                    "kotlin/**",
                    "**.bin"
                )
            }

            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_11
                targetCompatibility = JavaVersion.VERSION_11
            }
        }
    }
    plugins.withType(JavaPlugin::class.java) {
        extensions.configure(JavaPluginExtension::class.java) {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }
    }
}

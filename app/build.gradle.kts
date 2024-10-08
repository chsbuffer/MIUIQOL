plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.refine)
}

android {
    defaultConfig {
        applicationId = "io.github.chsbuffer.miuihelper"
        versionCode = 19
        versionName = "1.1.17"
        ndk {
            //noinspection ChromeOsAbiSupport
            abiFilters.add("arm64-v8a")
        }
    }
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
//            isMinifyEnabled = true
//            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
//            signingConfig = signingConfigs["debug"]
        }
    }
    kotlinOptions {
        jvmTarget = "11"
        compileOptions {
            freeCompilerArgs += listOf(
                "-Xno-param-assertions",
                "-Xno-receiver-assertions",
                "-Xno-call-assertions"
            )
        }
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    namespace = "io.github.chsbuffer.miuihelper"
}

dependencies {
    //noinspection KtxExtensionAvailable
    compileOnly(libs.androidx.preference)
    implementation(libs.dexkit)
    implementation(libs.androidx.annotation)
    compileOnly(libs.xposed.api)
    compileOnly(project(":miuistub"))

    implementation(libs.refine.runtime)
    implementation(libs.kotlin.stdlib.jdk8)
}
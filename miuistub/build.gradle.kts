plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "miui.os"
}

dependencies {
    annotationProcessor(libs.refine.annotation.processor)
    compileOnly(libs.refine.annotation)
    compileOnly(libs.androidx.preference)
}
plugins {
    id("drawer.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.urunkarpm.drawer.core.model"
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}

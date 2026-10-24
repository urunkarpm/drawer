plugins {
    id("drawer.android.library")
    id("drawer.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.urunkarpm.drawer.core.needle"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}

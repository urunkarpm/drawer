plugins {
    id("drawer.android.library")
    id("drawer.android.hilt")
}

android {
    namespace = "com.urunkarpm.drawer.core.common"
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}

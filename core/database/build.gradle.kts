plugins {
    id("drawer.android.library")
    id("drawer.android.room")
    id("drawer.android.hilt")
}

android {
    namespace = "com.urunkarpm.drawer.core.database"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}

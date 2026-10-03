plugins {
    id("drawer.android.feature")
}

android {
    namespace = "com.urunkarpm.drawer.feature.dock"
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:datastore"))
    implementation(libs.coil.compose)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}

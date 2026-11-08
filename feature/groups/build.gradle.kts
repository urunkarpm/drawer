plugins {
    id("drawer.android.feature")
}

android {
    namespace = "com.urunkarpm.drawer.feature.groups"
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:datastore"))
    implementation(libs.coil.compose)
    implementation(libs.compose.material.icons.extended)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}

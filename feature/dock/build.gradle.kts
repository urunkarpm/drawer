plugins {
    id("drawer.android.feature")
}

android {
    namespace = "com.urunkarpm.drawer.feature.dock"
}

dependencies {
    implementation(project(":core:data"))
    implementation(libs.coil.compose)
    testImplementation(libs.junit)
}

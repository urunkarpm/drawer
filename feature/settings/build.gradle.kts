plugins {
    id("drawer.android.feature")
}

android {
    namespace = "com.urunkarpm.drawer.feature.settings"
}

dependencies {
    implementation(project(":core:data"))
    testImplementation(libs.junit)
}

plugins {
    id("drawer.android.library")
    id("drawer.android.library.compose")
}

android {
    namespace = "com.urunkarpm.drawer.core.designsystem"
}

dependencies {
    implementation(project(":core:common"))
    testImplementation(libs.junit)
}

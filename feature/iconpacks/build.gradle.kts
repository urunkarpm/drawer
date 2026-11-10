plugins {
    id("drawer.android.feature")
}

android {
    namespace = "com.urunkarpm.drawer.feature.iconpacks"
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:datastore"))
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}

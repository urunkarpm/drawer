plugins {
    id("drawer.android.feature")
}

android {
    namespace = "com.urunkarpm.drawer.feature.home"
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:datastore"))
    implementation(project(":feature:dock"))
    implementation(project(":feature:groups"))
    implementation(project(":feature:notifications"))
    implementation(project(":feature:iconpacks"))
    implementation(project(":feature:settings"))

    implementation(libs.coil.compose)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}

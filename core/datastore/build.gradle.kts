plugins {
    id("drawer.android.library")
    id("drawer.android.hilt")
}

android {
    namespace = "com.urunkarpm.drawer.core.datastore"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(libs.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

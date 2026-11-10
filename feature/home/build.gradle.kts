plugins {
    id("drawer.android.feature")
}

android {
    namespace = "com.urunkarpm.drawer.feature.home"

    lint {
        // The baseline freezes the issues that existed before this cleanup so that new
        // ones fail the build. The errors it covers are MissingPermission false positives:
        // DuoStatusIcon reads telephony/connectivity state inside runCatching blocks with
        // permission-aware fallbacks, so a denied permission degrades the icon instead of
        // crashing. Lint cannot follow that pattern.
        // NewApi is deliberately NOT baselined: those are real crash risks, fixed in code.
        baseline = file("lint-baseline.xml")
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:datastore"))
    implementation(project(":feature:dock"))
    implementation(project(":feature:groups"))
    implementation(project(":feature:notifications"))
    implementation(project(":feature:iconpacks"))
    implementation(project(":feature:settings"))

    implementation(libs.coil.compose)
    implementation(libs.compose.material.icons.extended)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.serialization.json)
}

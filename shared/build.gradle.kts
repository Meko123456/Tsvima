plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "io.github.meko123456.tsvima.shared"
        compileSdk = 37
        minSdk = 26
        withHostTestBuilder {}
    }

    // A real iPhone (iosArm64) and the simulator CI runs the tests on (iosSimulatorArm64).
    // No iosX64: that is the Intel-Mac simulator, and neither the macOS runner nor any machine
    // here is Intel, so it would be a target nobody ever builds — which is how "iOS-ready"
    // became a claim this module could not back up in the first place.
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            // The second dependency this module has, and it earns its place: the rule for "which
            // forecast hours are still ahead of us" is calendar arithmetic against the *forecast
            // location's* clock, and it used to live in androidApp written against java.time —
            // which meant iOS either had no next-rain line or a second copy of the rule in Swift.
            // implementation, not api: Upcoming's public surface takes and returns no date type,
            // so this stays out of both the published API and the iOS framework's headers.
            implementation(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

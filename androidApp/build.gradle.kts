plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "io.github.meko123456.tsvima"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.meko123456.tsvima"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":shared"))

    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.okhttp)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.glance.appwidget)
    // Tsvima schedules nothing itself; Glance runs the widget's sessions on WorkManager and asks
    // for 2.7.1, which brings Room 2.2.5. Under R8 full mode that Room's keep rules no longer hold
    // the constructor it reaches for by reflection, so the minified app died in startup creating
    // WorkDatabase. Asking for a current WorkManager lifts both, and matches the rest of the fleet.
    implementation(libs.androidx.work.runtime)

    testImplementation(libs.junit)
}

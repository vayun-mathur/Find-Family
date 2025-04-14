plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.opengps.locationsharing.android"
    compileSdk = 35
    defaultConfig {
        applicationId = "cc.findfamily.app"
        minSdk = 34
        targetSdk = 35
        versionCode = 1
        versionName = "v1.0"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
    implementation(projects.shared)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.datastore.preferences.core.jvm)
    implementation(libs.androidx.activity)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)

}
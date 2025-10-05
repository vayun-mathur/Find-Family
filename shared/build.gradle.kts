import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    kotlin("plugin.serialization") version "2.0.0"
    alias(libs.plugins.composeMultiplatform)
    kotlin("plugin.compose")
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

kotlin {
    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_1_8)
                }
            }
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "shared"
            isStatic = true

            // Required when using NativeSQLiteDriver
            linkerOpts.add("-lsqlite3")
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.components.resources)
            implementation(libs.androidx.room.runtime)
            implementation(libs.kotlinx.datetime)
            implementation(libs.navigation.compose)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.ui)
            implementation(compose.components.uiToolingPreview)
            implementation(compose.components.resources)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.cryptography.core)

            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
            implementation(libs.maplibre.compose)

            implementation(libs.filekit.core)
            implementation(libs.filekit.dialogs)
            implementation(libs.filekit.dialogs.compose)
            implementation(libs.filekit.coil)
            implementation("org.jetbrains.compose.ui:ui-backhandler:1.10.0-alpha01")
            implementation(libs.compass.geocoder)
            implementation(libs.compass.geocoder.mobile)


        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.cryptography.provider.jdk)

            implementation(libs.androidx.activity)
            implementation(libs.androidx.activity.ktx)
            implementation(libs.androidx.fragment.ktx)
            implementation(libs.androidx.room.paging)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.cryptography.provider.apple)
            implementation(libs.androidx.sqlite.bundled)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

//cocoapods {
//    pod("MapLibre", "6.9.0")
//}

android {
    namespace = "com.opengps.locationsharing"
    compileSdk = 36
    defaultConfig {
        minSdk = 31
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(listOf(
            "-opt-in=kotlin.time.ExperimentalTime"
        ))
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
    add("kspIosX64", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    implementation(libs.androidx.runtime.android)
    implementation(libs.androidx.core)
}
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
            //location
            implementation(libs.compass.geolocation)
            implementation(libs.compass.geolocation.mobile)
            implementation(libs.compass.permissions.mobile)

            implementation(libs.androidx.room.runtime)
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
            implementation(libs.navigation.compose)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            //put your multiplatform dependencies here
            implementation(compose.components.resources)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(libs.mapcompose.mp)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation("androidx.datastore:datastore-preferences-core:1.1.3")
            implementation("dev.whyoleg.cryptography:cryptography-core:0.4.0")

            implementation("io.coil-kt.coil3:coil-compose:3.1.0")
            implementation("io.coil-kt.coil3:coil-network-ktor3:3.1.0")

        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation("dev.whyoleg.cryptography:cryptography-provider-jdk:0.4.0")

            implementation(libs.androidx.activity)
            implementation(libs.androidx.activity.ktx)
            implementation(libs.androidx.fragment.ktx)
            implementation(libs.androidx.room.paging)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation("dev.whyoleg.cryptography:cryptography-provider-apple:0.4.0")
            implementation("androidx.sqlite:sqlite-bundled:2.5.0-rc02")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.opengps.locationsharing"
    compileSdk = 34
    defaultConfig {
        minSdk = 34
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
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

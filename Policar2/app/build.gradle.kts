// ═══════════════════════════════════════════════════════════════════════
//  POLICAR — build.gradle.kts (app)
//
//  SOLUCIÓN: Actualizado para compatibilidad con Polar SDK 7.1.0 y
//  Jetpack Compose moderno. Se corrige el namespace y se añade el bypass
//  de versión de metadata de Kotlin para el SDK de Polar.
// ═══════════════════════════════════════════════════════════════════════

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.policar"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.policar"
        minSdk = 33
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
        // Necesario para coroutines con suspendCancellableCoroutine
        // Y bypass para la versión de metadata del SDK de Polar
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-Xskip-metadata-version-check"
        )
    }

    buildFeatures {
        compose = true
    }

    // CRÍTICO: evitar conflictos de clases duplicadas entre RxJava y Coroutines
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/io.netty.versions.properties"
        }
    }
}

dependencies {
    // ── Core Android ─────────────────────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // ── Compose ───────────────────────────────────────────────────────
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.accompanist.permissions)

    // ── Supabase & Ktor ───────────────────────────────────────────────
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.auth)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    // ── Serialization ─────────────────────────────────────────────────
    implementation(libs.kotlinx.serialization.json)

    // ── Coroutines ────────────────────────────────────────────────────
    implementation(libs.kotlinx.coroutines.android)
    // NECESARIO para rxjava3 → coroutines bridge (suspendCancellableCoroutine con RxJava)
    implementation(libs.kotlinx.coroutines.rx3)

    // ── Polar SDK ─────────────────────────────────────────────────────
    implementation(libs.polar.sdk)

    // ── RxJava 3 (requerido por Polar SDK) ────────────────────────────
    implementation(libs.rxjava)
    implementation(libs.rxandroid)

    // ── DataStore (para persistir deviceId) ───────────────────────────
    implementation(libs.androidx.datastore.preferences)

    // ── Testing ───────────────────────────────────────────────────────
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

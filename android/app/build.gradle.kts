import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Supabase config comes from android/local.properties (git-ignored).
// Defaults target a local `supabase start` via `adb reverse tcp:54321 tcp:54321`.
val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
fun prop(name: String, default: String) = localProps.getProperty(name) ?: default

android {
    namespace = "com.moni.app"
    compileSdk { version = release(37) { minorApiLevel = 2 } }

    defaultConfig {
        applicationId = "com.moni.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField("String", "SUPABASE_URL", "\"${prop("supabase.url", "http://127.0.0.1:54321")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${prop("supabase.anonKey", "")}\"")
    }

    // Release signing from android/local.properties (keystore lives outside the repo).
    // Without those keys the release build falls back to the debug key.
    signingConfigs {
        create("release") {
            prop("release.storeFile", "").takeIf { it.isNotEmpty() }?.let { storeFile = file(it) }
            storePassword = prop("release.storePassword", "")
            keyAlias = prop("release.keyAlias", "")
            keyPassword = prop("release.keyPassword", "")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (localProps.containsKey("release.storeFile")) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.lucide)
    debugImplementation(libs.compose.ui.tooling)

    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.kotlinx.serialization.json)
}

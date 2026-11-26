import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// AGP 9 incluye Kotlin integrado: no se aplica org.jetbrains.kotlin.android.
// El anuncio del BOM de Compose 2026.11 indica que exige compileSdk 37.1. Si AGP rechazara esta sintaxis,
// la alternativa es `compileSdk = 37`; el error de CI dirá cuál de las dos acepta.
android {
    namespace = "es.ahorra.app"
    compileSdk {
        version = release(37) { minorApiLevel = 1 }
    }

    defaultConfig {
        applicationId = "es.ahorra.app"
        minSdk = 26
        targetSdk = 36 // Google Play exige API 36+ para apps nuevas desde el 31/08/2026.
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // Clave de depuración ESTABLE para uso personal: permite actualizar la app encima de la anterior.
        // No es secreta ni sirve para Google Play (la subida usa la clave de CI de abajo).
        create("personal") {
            storeFile = file("ahorra-debug.keystore")
            storePassword = "ahorradebug"
            keyAlias = "ahorra"
            keyPassword = "ahorradebug"
        }
        // La firma real se inyecta por variables de entorno (CI). Nunca hay secretos en el repositorio.
        val keystorePath = System.getenv("AHORRA_KEYSTORE_FILE")
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("AHORRA_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("AHORRA_KEY_ALIAS")
                keyPassword = System.getenv("AHORRA_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("personal")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (System.getenv("AHORRA_KEYSTORE_FILE") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    lint {
        abortOnError = true
        warningsAsErrors = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.biometric)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlin.test.junit)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

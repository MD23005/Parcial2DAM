
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.ksp)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")

if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { stream ->
        localProperties.load(stream)
    }
}

android {
    namespace = "dam.project.parcial2dam"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "dam.project.parcial2dam"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["MAPS_API"] =
            localProperties.getProperty("MAPS_API")
                ?: "CLAVE_FALTANTE"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // Componentes de Android
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation("androidx.core:core-splashscreen:1.0.0")

    // Firebase
    implementation(
        platform("com.google.firebase:firebase-bom:33.7.0")
    )
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-auth")

    // Encriptación de contraseñas
    implementation("org.mindrot:jbcrypt:0.4")

    // Room: base de datos local
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // WorkManager: sincronización de citas
    implementation("androidx.work:work-runtime:2.9.1")

    // Google Maps
    implementation("com.google.android.gms:play-services-maps:18.2.0")

    // Pruebas
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}

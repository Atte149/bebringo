plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

val appsDebugKeystore = providers.gradleProperty("APPS_DEBUG_KEYSTORE").orNull
val appsKeystore = providers.gradleProperty("APPS_KEYSTORE").orNull
val appsStorePassword = providers.gradleProperty("APPS_STORE_PASSWORD").orNull
val appsKeyAlias = providers.gradleProperty("APPS_KEY_ALIAS").orNull
val appsKeyPassword = providers.gradleProperty("APPS_KEY_PASSWORD").orNull

android {
    namespace = "org.dueattendant149.bebringo"
    compileSdk = 35

    defaultConfig {
        applicationId = "org.dueattendant149.bebringo"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        if (appsDebugKeystore != null) {
            create("sharedDebug") {
                storeFile = File(appsDebugKeystore)
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
        if (appsKeystore != null && appsStorePassword != null && appsKeyAlias != null && appsKeyPassword != null) {
            create("release") {
                storeFile = File(appsKeystore)
                storePassword = appsStorePassword
                keyAlias = appsKeyAlias
                keyPassword = appsKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.findByName("sharedDebug") ?: signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
                ?: error("APPS_KEYSTORE is not configured in ~/.gradle/gradle.properties")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)

    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.yaml)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.okhttp)
    implementation(libs.androidx.work.runtime)

    testImplementation(libs.junit)
}

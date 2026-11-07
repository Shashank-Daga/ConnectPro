
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val configuredApiBaseUrl = providers
    .gradleProperty("CONNECTPRO_API_BASE_URL")
    .orElse("http://10.0.2.2:8080/")
    .get()
    .let { if (it.endsWith("/")) it else "$it/" }

val configuredReleaseApiBaseUrl = providers
    .gradleProperty("CONNECTPRO_API_BASE_URL")
    .orNull
    ?.trim()
    .orEmpty()

val normalizedReleaseApiBaseUrl =
    if (configuredReleaseApiBaseUrl.isBlank()) {
        ""
    } else if (configuredReleaseApiBaseUrl.endsWith("/")) {
        configuredReleaseApiBaseUrl
    } else {
        "$configuredReleaseApiBaseUrl/"
    }

// Release signing credentials are read from environment variables.
// Never store these values in source code or commit them to Git.
val releaseKeystorePath =
    providers.environmentVariable("CONNECTPRO_KEYSTORE_PATH").orNull.orEmpty()

val releaseKeyAlias =
    providers.environmentVariable("CONNECTPRO_KEY_ALIAS").orNull.orEmpty()

val releaseStorePassword =
    providers.environmentVariable("CONNECTPRO_STORE_PASSWORD").orNull

val releaseKeyPassword =
    providers.environmentVariable("CONNECTPRO_KEY_PASSWORD").orNull

val validateReleaseApiBaseUrl = tasks.register(
    "validateReleaseApiBaseUrl"
) {
    group = "verification"
    description =
        "Validates that release builds use a configured HTTPS API URL."

    doLast {
        if (configuredReleaseApiBaseUrl.isBlank()) {
            throw org.gradle.api.GradleException(
                "Release builds require CONNECTPRO_API_BASE_URL " +
                        "to be explicitly configured with the production HTTPS URL."
            )
        }

        if (!configuredReleaseApiBaseUrl.startsWith(
                "https://",
                ignoreCase = true
            )
        ) {
            throw org.gradle.api.GradleException(
                "Release builds require CONNECTPRO_API_BASE_URL " +
                        "to start with https://."
            )
        }
    }
}

val validateReleaseSigningConfig = tasks.register(
    "validateReleaseSigningConfig"
) {
    group = "verification"
    description = "Validates the Android release signing configuration."

    doLast {
        if (releaseKeystorePath.isBlank()) {
            throw org.gradle.api.GradleException(
                "Missing CONNECTPRO_KEYSTORE_PATH environment variable."
            )
        }

        if (!file(releaseKeystorePath).isFile) {
            throw org.gradle.api.GradleException(
                "Release keystore file was not found at the configured path."
            )
        }

        if (releaseKeyAlias.isBlank()) {
            throw org.gradle.api.GradleException(
                "Missing CONNECTPRO_KEY_ALIAS environment variable."
            )
        }

        if (releaseStorePassword.isNullOrBlank()) {
            throw org.gradle.api.GradleException(
                "Missing CONNECTPRO_STORE_PASSWORD environment variable."
            )
        }

        if (releaseKeyPassword.isNullOrBlank()) {
            throw org.gradle.api.GradleException(
                "Missing CONNECTPRO_KEY_PASSWORD environment variable."
            )
        }
    }
}

// Validate only when Gradle is building the release variant.
// Debug builds can continue using the local emulator URL.
tasks.matching {
    it.name == "preReleaseBuild"
}.configureEach {
    dependsOn(validateReleaseApiBaseUrl)
    dependsOn(validateReleaseSigningConfig)
}

android {
    namespace = "com.connectpro"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.connectpro"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "API_BASE_URL",
            "\"$configuredApiBaseUrl\""
        )
    }

    signingConfigs {
        create("release") {
            storeFile = if (releaseKeystorePath.isBlank()) {
                null
            } else {
                file(releaseKeystorePath)
            }

            storePassword = releaseStorePassword
            keyAlias = releaseKeyAlias
            keyPassword = releaseKeyPassword
        }
    }

    buildTypes {
        debug {
            // Development only: permits a local HTTP backend.
            manifestPlaceholders["usesCleartextTraffic"] = "true"
        }

        release {
            // Use the dedicated release signing key.
            signingConfig = signingConfigs.getByName("release")

            // Release builds must configure CONNECTPRO_API_BASE_URL.
            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"$normalizedReleaseApiBaseUrl\""
            )

            manifestPlaceholders["usesCleartextTraffic"] = "false"

            optimization {
                enable = false
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)

    testImplementation(libs.junit)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

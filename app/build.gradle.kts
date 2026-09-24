// App-module build file.
// This is where we declare the app's configuration (package, SDK versions, signing, etc.)
// and the libraries the app depends on.

import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// --- Release signing credentials -----------------------------------------------------
// Android requires a release build (the APK/AAB you'd upload to the Play Store) to be
// cryptographically signed, so installs/updates can be verified as coming from the same
// developer; debug builds skip this and use an auto-generated, non-secret debug key instead.
//
// Credentials are read from environment variables first (CI-friendly), falling back to a
// local `keystore.properties` file at the repo root (handy on a dev machine). Neither source
// is ever committed: keystore.properties, *.jks, and *.keystore are all gitignored, and no
// value read here is printed or logged.
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) {
        keystorePropsFile.inputStream().use { load(it) }
    }
}

fun releaseSigningValue(envVar: String, propertyKey: String): String? {
    val fromEnv = System.getenv(envVar)
    if (!fromEnv.isNullOrBlank()) return fromEnv
    return keystoreProps.getProperty(propertyKey)?.takeIf { it.isNotBlank() }
}

val releaseStoreFilePath = releaseSigningValue("TIDELET_KEYSTORE_PATH", "storeFile")
val releaseStorePassword = releaseSigningValue("TIDELET_KEYSTORE_PASSWORD", "storePassword")
val releaseKeyAlias = releaseSigningValue("TIDELET_KEY_ALIAS", "keyAlias")
val releaseKeyPassword = releaseSigningValue("TIDELET_KEY_PASSWORD", "keyPassword")

// Only true once every credential was actually found (env vars and/or keystore.properties).
val hasReleaseSigningCredentials = releaseStoreFilePath != null &&
    releaseStorePassword != null &&
    releaseKeyAlias != null &&
    releaseKeyPassword != null

android {
    namespace = "com.tidelet.app"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.tidelet.app"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "com.tidelet.app.testutil.TideletTestRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    // Share test doubles (FakeTideletRepository, TestTideletApplication, FixedClocks)
    // between JVM unit tests and instrumented tests.
    sourceSets {
        getByName("test").java.srcDir("src/sharedTest/java")
        getByName("androidTest").java.srcDir("src/sharedTest/java")
    }

    signingConfigs {
        // A signingConfig bundles the keystore file + credentials Gradle signs a release
        // build with. Only created when credentials were actually found above, so this
        // stays absent for contributors/CI runs that just build debug.
        if (hasReleaseSigningCredentials) {
            create("release") {
                storeFile = file(releaseStoreFilePath!!)
                storePassword = releaseStorePassword
                // keyAlias picks which key inside the keystore to sign with (a keystore can
                // hold more than one); keyPassword unlocks that specific key.
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Leave signingConfig unset if no credentials were found, rather than failing
            // Gradle's configuration phase — that keeps `./gradlew assembleDebug` (and CI)
            // working for anyone without a keystore set up. `assembleRelease`/`bundleRelease`
            // will still fail without it, but with Android's own clear "signing config
            // required" error, not a cryptic NPE or missing-file crash.
            if (hasReleaseSigningCredentials) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            // Use the debug applicationId suffix so a debug build can coexist with a release build.
            applicationIdSuffix = ".debug"
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
        // AGP 8+ no longer generates BuildConfig by default — opt in so
        // BuildConfig.DEBUG exists (used to gate the debug-only demo data
        // seeder; see TideletApplication.onCreate()).
        buildConfig = true
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "META-INF/LICENSE*",
            )
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Room (local database)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore (user preferences)
    implementation(libs.androidx.datastore.preferences)

    // WorkManager (for future reminder jobs)
    implementation(libs.androidx.work.runtime.ktx)

    // SplashScreen compat — cold-start splash with an animated icon.
    implementation(libs.androidx.core.splashscreen)

    // Debug-only tooling
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Unit tests (src/test/)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core.ktx)
    testImplementation(libs.google.truth)

    // Instrumented tests (src/androidTest/)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.core.ktx)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.turbine)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.google.truth)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}

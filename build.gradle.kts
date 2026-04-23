// Top-level ("project-level") build file.
// We don't declare dependencies for the app here — just register the plugins we'll use
// so the `:app` module (and any future modules) can apply them.

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}

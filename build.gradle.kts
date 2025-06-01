buildscript {
    dependencies {
        // ✅ Add Google Services plugin
        classpath("com.google.gms:google-services:4.4.2")
    }
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}

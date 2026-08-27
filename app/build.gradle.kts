plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.riftking.mirrorcounter"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.riftking.mirrorcounter"
        minSdk = 30
        targetSdk = 36
        versionCode = 2
        versionName = "2.0-r860-appliance"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("androidx.activity:activity-ktx:1.13.0")
    implementation("androidx.wear:wear:1.3.0")
}

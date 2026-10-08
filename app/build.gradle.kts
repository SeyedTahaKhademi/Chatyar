import java.net.URI
import java.security.MessageDigest

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.kapt")
}

// Pinned sing-box libbox engine. Large native binary is acquired at build time.
// The SHA-256 value is taken from the public TypeNil/vpnclient-android core pin.
val coreVersion = "1.14.1"
val coreSha256 = "93b2596c4e90df32463a9ade5c89d85f83a16aacd94928ba26fc4bce41eaf2a9"
val libboxFile = rootProject.file("core-native/libbox-$coreVersion.aar")
val downloadLibbox by tasks.registering {
    val output = libboxFile
    val version = coreVersion
    val expected = coreSha256
    outputs.file(output)
    onlyIf { !output.exists() }
    doLast {
        output.parentFile.mkdirs()
        val temp = File.createTempFile("libbox-", ".aar", output.parentFile)
        try {
            URI("https://github.com/singbox-android/libbox/releases/download/$version/libbox.aar")
                .toURL().openStream().use { input -> temp.outputStream().use { input.copyTo(it) } }
            val sha = MessageDigest.getInstance("SHA-256")
            temp.inputStream().use { input ->
                val buf = ByteArray(1024 * 1024)
                while (true) { val len = input.read(buf); if (len < 0) break; sha.update(buf, 0, len) }
            }
            val actual = sha.digest().joinToString("") { "%02x".format(it) }
            require(actual == expected) { "libbox SHA-256 mismatch" }
            check(temp.renameTo(output)) { "Cannot move libbox into core-native" }
        } finally { temp.delete() }
    }
}
val verifyLibbox by tasks.registering {
    val input = libboxFile
    val expected = coreSha256
    dependsOn(downloadLibbox)
    inputs.file(input)
    doLast {
        val sha = MessageDigest.getInstance("SHA-256")
        input.inputStream().use { stream ->
            val buf = ByteArray(1024 * 1024)
            while (true) { val len = stream.read(buf); if (len < 0) break; sha.update(buf, 0, len) }
        }
        val actual = sha.digest().joinToString("") { "%02x".format(it) }
        check(actual == expected) { input.delete(); "Cached libbox failed integrity check" }
    }
}
tasks.named("preBuild") { dependsOn(verifyLibbox) }

android {
    namespace = "ir.hooshamoozan.chatyar"
    compileSdk = 35

    defaultConfig {
        applicationId = "ir.hooshamoozan.chatyar"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "0.3.1-beta"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.ui:ui-text-google-fonts")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.core:core:1.15.0")
    implementation("org.yaml:snakeyaml:2.3")
    implementation(files(libboxFile))
}

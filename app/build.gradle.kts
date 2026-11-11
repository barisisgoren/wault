plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "app.wault"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "app.wault"
        minSdk = 28
        targetSdk = 36
        versionCode = 2
        versionName = "0.2.0"
    }

    buildTypes {
        release {
            optimization {
                enable = false
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
    testOptions {
        unitTests {
            // Robolectric: kaynaklar (yazı tipleri, ikonlar) testte de yüklensin
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.barcode)
    implementation(libs.zxing.core)
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
    testImplementation("org.json:json:20240303")
    // Arayüz testleri telefonsuz, bilgisayarda çalışır (Robolectric + Compose testleri)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// Windows kullanıcı klasörü ASCII dışı karakter içerdiğinde JDK'nın yerel soket dosyaları
// varsayılan temp klasöründe açılamıyor; test JVM'ine proje içi bir klasör veriyoruz.
tasks.withType<Test>().configureEach {
    val tmp = rootProject.layout.projectDirectory.dir(".tmp").asFile
    jvmArgs("-Djdk.net.unixdomain.tmpdir=${tmp.absolutePath}", "-Djava.io.tmpdir=${tmp.absolutePath}")
    // Roborazzi: ekran görüntülerini her çalıştırmada app/build/test-screenshots altına yaz
    systemProperty("roborazzi.test.record", "true")
}

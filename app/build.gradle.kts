plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
}

android {
  namespace = "xyz.junerver.android.lockdemo"
  compileSdk = 36

  defaultConfig {
    applicationId = "xyz.junerver.android.lockdemo"
    minSdk = 24
    targetSdk = 35
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  kotlinOptions {
    jvmTarget = "11"
  }
  testOptions {
    unitTests.isReturnDefaultValues = true
  }
}

dependencies {
  testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
  testImplementation(kotlin("test"))
  testImplementation("org.robolectric:robolectric:4.11.1")

  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.appcompat)
  implementation(libs.material)
  implementation(libs.androidx.activity)
  implementation(libs.androidx.constraintlayout)

  // 添加Gson支持JSON解析
  implementation("com.google.code.gson:gson:2.10.1")
  // 串口通信库
  implementation("com.github.kongqw:AndroidSerialPort:1.0.0")
}
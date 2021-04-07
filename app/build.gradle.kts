plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("android.extensions")
    kotlin("kapt")
}


android {
    compileSdkVersion(30)

    defaultConfig {
        applicationId("org.openvoipalliance.voiplibexample")
        minSdkVersion(26)
        targetSdkVersion(30)
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        addDefaultAuthValues(this)
        multiDexEnabled = true
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    splits {
        abi {
            isEnable = true
            isUniversalApk = false
        }
    }
}

dependencies {
    val roomVersion = "2.2.6"

    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("androidx.appcompat:appcompat:1.2.0")
    implementation("androidx.constraintlayout:constraintlayout:2.0.4")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.4.20")
    implementation("com.google.android.material:material:1.3.0")
    implementation("androidx.navigation:navigation-fragment:2.3.2")
    implementation("androidx.navigation:navigation-ui:2.3.2")
    implementation("androidx.navigation:navigation-fragment-ktx:2.3.2")
    implementation("androidx.navigation:navigation-ui-ktx:2.3.2")
    implementation("androidx.preference:preference-ktx:1.1.1")
    implementation("androidx.room:room-runtime:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    api(project(":AndroidVoIPLib"))

    androidTestImplementation("androidx.test.espresso:espresso-core:3.1.0") {
        exclude(group = "com.android.support", module = "support-annotations")
    }
    testImplementation("junit:junit:4.13.1")
}

fun addDefaultAuthValues(defaultConfig: com.android.build.api.dsl.DefaultConfig) {
    // If you wish to pre-populate the example app with authentication information to
    // make testing quicker, just add these properties (e.g. apl.default.username) to
    // your ~/.gradle/gradle.properties file.
    try {
        defaultConfig.resValue("string", "default_sip_user", project.property("avl.default.username") as String)
        defaultConfig.resValue("string", "default_sip_password", project.property("avl.default.password") as String)
        defaultConfig.resValue("string", "default_sip_domain", project.property("avl.default.domain") as String)
        defaultConfig.resValue("string", "default_sip_port", project.property("avl.default.port") as String)
    } catch (e: groovy.lang.MissingPropertyException) {
        defaultConfig.resValue("string", "default_sip_user", "")
        defaultConfig.resValue("string", "default_sip_password", "")
        defaultConfig.resValue("string", "default_sip_domain", "")
        defaultConfig.resValue("string", "default_sip_port", "")
    }
}
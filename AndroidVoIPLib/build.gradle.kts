plugins {
    id("com.android.library")
    kotlin("android")
    id("maven-publish")
    id("com.kezong.fat-aar")
}

val libraryVersion = "0.6.28"

android {
    compileSdk = 30

    defaultConfig {
        minSdk = 26
        targetSdk = 30
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("androidx.appcompat:appcompat:1.3.1")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:${rootProject.extra["kotlinVersion"]}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.5.0")
    implementation("com.google.code.gson:gson:2.8.6")
    implementation("androidx.media:media:1.4.3")

    api("io.insert-koin:koin-android:2.2.2")
    embed("org.linphone.minimal:linphone-sdk-android:5.0.49")
}

publishing {
    publications {
        create<MavenPublication>("Production") {
            artifact("$buildDir/outputs/aar/AndroidVoIPLib-release.aar")
            groupId = "org.openvoipalliance"
            artifactId = "AndroidPhoneLib"
            version = libraryVersion

            pom.withXml {
                val dependenciesNode = asNode().appendNode("dependencies")

                configurations.implementation.allDependencies.forEach {
                    if (it.name != "unspecified") {
                        val dependencyNode = dependenciesNode.appendNode("dependency")
                        dependencyNode.appendNode("groupId", it.group)
                        dependencyNode.appendNode("artifactId", it.name)
                        dependencyNode.appendNode("version", it.version)
                    }
                }
            }
        }
    }
}

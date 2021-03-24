import java.util.*

plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("android.extensions")
    id("com.kezong.fat-aar")
    id("maven-publish")
    id("com.jfrog.bintray")
}

val libraryVersion = "0.6.26"

android {
    compileSdkVersion(30)
    defaultConfig {
        minSdkVersion(23)
        targetSdkVersion(30)
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("androidx.appcompat:appcompat:1.2.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:${rootProject.extra["kotlinVersion"]}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.3.6")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.1.0")
    testImplementation("junit:junit:4.12")

    api("org.koin:koin-android:2.2.0")
    embed("org.linphone:linphone-sdk-android:4.5.0-beta.117+4925ad5")
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

fun findProperty(s: String) = project.findProperty(s) as String?

bintray {
    user = findProperty("bintray.user")
    key = findProperty("bintray.token")
    setPublications("Production")
    pkg(delegateClosureOf<com.jfrog.bintray.gradle.BintrayExtension.PackageConfig> {
        repo = "AndroidPhoneLib"
        name = "AndroidPhoneLib"
        websiteUrl = "https://github.com/open-voip-alliance/AndroidPhoneLib"
        githubRepo = "open-voip-alliance/AndroidPhoneLib"
        vcsUrl = "https://github.com/open-voip-alliance/AndroidPhoneLib"
        description = "An Android library to facilitate SIP communication."
        setLabels("kotlin")
        setLicenses("Apache-2.0")
        publish = true
        desc = description
        version(delegateClosureOf<com.jfrog.bintray.gradle.BintrayExtension.VersionConfig> {
            name = libraryVersion
            released = Date().toString()
        })
    })
}

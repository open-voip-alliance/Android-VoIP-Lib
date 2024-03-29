// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    project.extra.set("kotlinVersion",  "1.4.10")

    repositories {
        mavenCentral()
        google()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:4.2.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${project.extra["kotlinVersion"]}")
        classpath("com.github.kezong:fat-aar:1.3.6")
        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

allprojects {
    repositories {
        google()
        maven {
            url = uri("https://linphone.org/maven_repository/")
        }
        mavenCentral()
        maven("https://jitpack.io")
    }
}

tasks.register("clean",Delete::class){
    delete(rootProject.buildDir)
}
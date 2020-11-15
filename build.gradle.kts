// Top-level build file where you can add configuration options common to all sub-projects/modules.


buildscript {
    project.extra.set("kotlinVersion",  "1.3.72")

    repositories {
        jcenter()
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:4.1.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${project.extra["kotlinVersion"]}")
        classpath("com.android.tools.build:gradle:4.1.1")
        classpath("com.jfrog.bintray.gradle:gradle-bintray-plugin:1.4")
        classpath("com.kezong:fat-aar:1.2.19")
        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

allprojects {
    repositories {
        jcenter()
        google()
        maven {
            url = uri("https://linphone.org/maven_repository/")
        }
        mavenCentral()
    }
}

tasks.register("clean",Delete::class){
    delete(rootProject.buildDir)
}
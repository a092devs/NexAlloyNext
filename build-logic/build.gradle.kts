plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
}

group = "io.github.nexalloy.build"
version = providers.gradleProperty("nexalloyBuildLogicVersion").orElse("1.0.0").get()

dependencies {
    implementation("com.android.tools.build:gradle:${libs.versions.agp.get()}")
    // The Android Gradle Plugin brings the Kotlin Gradle plugin at runtime.
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin-api:2.2.10")
}

publishing {
    repositories {
        maven {
            name = "release"
            url = uri(rootProject.layout.projectDirectory.dir("../build/maven"))
        }
    }
}

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    id("com.vanniktech.maven.publish") version "0.35.0"
}

group = "io.github.rootachieve"
version = (findProperty("VERSION_NAME") as String?)
    ?: (findProperty("version") as String?)
    ?: "0.1.0-SNAPSHOT"

kotlin {
    androidTarget {
        publishLibraryVariants("release")
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    iosArm64()
    iosSimulatorArm64()

    js {
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.compose.runtime)
            api(libs.compose.foundation)
            api(libs.compose.ui)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.rootachieve.koraph.graphvisualizer"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates(group.toString(), "Koraph", version.toString())

    pom {
        name.set("Koraph Graph Visualizer")
        description.set("Compose Multiplatform graph visualization library for adjacency-list inputs.")
        inceptionYear.set("2026")
        url.set("https://github.com/rootachieve/Koraph")

        licenses {
            license {
                name.set("Apache License 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }

        developers {
            developer {
                id.set("rootachieve")
                name.set("rootachieve")
                email.set("rootachieve6053@gmail.com")
                organization.set("rootachieve")
                organizationUrl.set("https://github.com/rootachieve")
                url.set("https://github.com/rootachieve")
            }
        }

        scm {
            url.set("https://github.com/rootachieve/Koraph")
            connection.set("scm:git:git://github.com/rootachieve/Koraph.git")
            developerConnection.set("scm:git:ssh://git@github.com/rootachieve/Koraph.git")
        }
    }
}

import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.utils.loadPropertyFromResources
import org.jetbrains.kotlin.konan.properties.Properties
import org.jetbrains.kotlin.konan.properties.loadProperties

plugins {
    kotlin("jvm") version "1.6.10"
    id("org.jetbrains.compose") version "1.1.1"
    kotlin("plugin.serialization") version "1.4.10"
}

group = "me.koheisato"
version = "1.0.1"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    // compose-jb
    implementation(compose.desktop.currentOs)

    // json
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "AutoHeart"
            packageVersion = "1.0.1"
            copyright = "© 2023 Kohei Sato. All rights reserved."

            macOS {
                bundleID = "net.iobb.kohei.autoheart"
                signing {
                    sign.set(true)
                    identity.set("Kohei Sato")
                }
                notarization{
                    val prop = Properties()
                    prop.load(File("${project.rootDir}/certification.properties").inputStream())
                    appleID.set(prop.getProperty("appleId"))
                    password.set(prop.getProperty("password"))
                }
            }
        }
    }
}
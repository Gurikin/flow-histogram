plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.plugin.serialization)
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("application")
}

application {
    mainClass = "org.gurikin.WeightDisplayApp"
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation("org.openjfx:javafx-controls:17")
    implementation("org.openjfx:javafx-fxml:17")
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.logback.classic)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.config.yaml)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test)
}

javafx {
    version = "17"
    modules = listOf("javafx.controls")
}

tasks.test {
    useJUnitPlatform()
}

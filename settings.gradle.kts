enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "flow-histogram"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }
}

include(
    ":histogram",
    ":demo-3d"
)

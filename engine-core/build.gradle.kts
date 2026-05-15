plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    `java-library`
}

dependencies {
    implementation(libs.kotlin.reflect)

    api(libs.kotlinx.serialization.json)

    api(libs.joml)

    implementation(platform(libs.lwjgl.bom))
    api(libs.lwjgl.core)
    api(libs.lwjgl.stb)

    api(libs.imgui.binding)

    implementation(libs.reflections)

    api(libs.kotlinLogging)
    api(libs.logback.classic)
}

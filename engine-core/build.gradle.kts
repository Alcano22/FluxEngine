plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
}

dependencies {
    implementation(libs.kotlin.reflect)

    api(libs.joml)

    implementation(platform(libs.lwjgl.bom))
    api(libs.lwjgl.core)
    api(libs.lwjgl.stb)

    api(libs.imgui.binding)
}

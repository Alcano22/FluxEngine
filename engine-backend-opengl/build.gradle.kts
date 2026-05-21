plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
}

val osName = System.getProperty("os.name").lowercase()

dependencies {
    api(project(":engine-core"))

    implementation(platform(libs.lwjgl.bom))
    implementation(libs.lwjgl.core)
    implementation(libs.lwjgl.opengl)

    implementation(libs.imgui.lwjgl3)
    implementation(libs.imgui.natives.windows)
    implementation(libs.imgui.natives.linux)
    implementation(libs.imgui.natives.macos)
}

plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
}

val osName = System.getProperty("os.name").lowercase()

dependencies {
    api(project(":engine-core"))

    implementation(platform(libs.lwjgl.bom))
    implementation(libs.lwjgl.core)
    implementation(libs.lwjgl.glfw)

    implementation(libs.jamepad)

    implementation(libs.imgui.lwjgl3)
    runtimeOnly(when {
        osName.contains("win") -> libs.imgui.natives.windows
        osName.contains("mac") -> libs.imgui.natives.macos
        else                   -> libs.imgui.natives.linux
    })
}

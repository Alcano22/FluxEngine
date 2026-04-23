plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
}

dependencies {
    api(project(":engine-core"))

    implementation(platform(libs.lwjgl.bom))
    implementation(libs.lwjgl.core)
    implementation(libs.lwjgl.opengl)

    implementation(libs.imgui.lwjgl3)
    runtimeOnly(libs.imgui.natives.windows)
}

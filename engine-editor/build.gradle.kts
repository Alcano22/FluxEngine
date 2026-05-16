plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

val osName = System.getProperty("os.name").lowercase()
val osArch = System.getProperty("os.arch").lowercase()

val lwjglNatives = when {
    osName.contains("win") -> "natives-windows"
    osName.contains("mac") -> {
        if (osArch.contains("aarch64"))
            "natives-macos-arm64"
        else
            "natives-macos"
    }
    else -> "natives-linux"
}

dependencies {
    implementation(project(":engine-core"))
    implementation(project(":engine-backend-glfw"))
    implementation(project(":engine-backend-opengl"))
    implementation(project(":engine-scripting"))

    implementation(libs.kotlinx.coroutines)

    implementation(platform(libs.lwjgl.bom))
    runtimeOnly("org.lwjgl:lwjgl::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-glfw::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-opengl::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-stb::$lwjglNatives")
}

application {
    mainClass.set("org.flux.editor.MainKt")
}

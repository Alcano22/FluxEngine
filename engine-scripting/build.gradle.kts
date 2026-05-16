plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    `java-library`
}

dependencies {
    api(project(":engine-core"))

    implementation(libs.kotlin.reflect)
    implementation(libs.kotlin.compiler)
}


plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":packets"))
    testImplementation(project(":packets"))

    implementation(libs.kotlinx.coroutines.core)

    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
}
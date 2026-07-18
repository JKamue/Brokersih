plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":packets"))
    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter)
}

tasks.test {
    useJUnitPlatform()
}
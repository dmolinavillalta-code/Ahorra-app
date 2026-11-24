import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Núcleo de negocio: Kotlin/JVM puro, sin dependencias de Android ni de terceros.
// Así se puede compilar y probar en cualquier máquina con JDK 17+, sin Android SDK.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

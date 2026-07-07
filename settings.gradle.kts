rootProject.name = "edujacksons"

// Foojay resolver: позволяет Gradle автоматически подтянуть JDK нужной версии
// (Java 21 toolchain) на машинах/CI, где его нет в PATH.
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

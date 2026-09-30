plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
}

// detekt and ktlint run as standalone CLI tools. This keeps them independent of the
// AGP / Kotlin plugin versions: `./gradlew detekt ktlintCheck`, `./gradlew ktlintFormat`.
val detektCli: Configuration = configurations.create("detektCli")
val ktlintCli: Configuration = configurations.create("ktlintCli")

dependencies {
    detektCli(libs.detekt.cli)
    ktlintCli(libs.ktlint.cli) {
        attributes {
            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
        }
    }
}

val kotlinSources = fileTree("app/src") { include("**/*.kt") }
val ktlintPatterns = listOf("app/src/**/*.kt", "*.kts", "app/*.kts")

tasks.register<JavaExec>("detekt") {
    group = "verification"
    description = "Runs detekt static analysis over the app sources."
    classpath = detektCli
    mainClass.set("io.gitlab.arturbosch.detekt.cli.Main")
    inputs.files(kotlinSources)
    inputs.file("config/detekt/detekt.yml")
    outputs.dir(layout.buildDirectory.dir("reports/detekt"))
    args(
        "--input", "app/src",
        "--config", "config/detekt/detekt.yml",
        "--build-upon-default-config",
        "--parallel",
        "--report", "html:build/reports/detekt/detekt.html",
        "--report", "txt:build/reports/detekt/detekt.txt",
    )
}

tasks.register<JavaExec>("ktlintCheck") {
    group = "verification"
    description = "Checks Kotlin code style with ktlint."
    classpath = ktlintCli
    mainClass.set("com.pinterest.ktlint.Main")
    inputs.files(kotlinSources)
    inputs.file(".editorconfig")
    outputs.dir(layout.buildDirectory.dir("reports/ktlint"))
    args(ktlintPatterns + "--reporter=plain" + "--reporter=checkstyle,output=build/reports/ktlint/ktlint.xml")
}

tasks.register<JavaExec>("ktlintFormat") {
    group = "formatting"
    description = "Fixes Kotlin code style violations with ktlint."
    classpath = ktlintCli
    mainClass.set("com.pinterest.ktlint.Main")
    jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
    args(listOf("-F") + ktlintPatterns)
}

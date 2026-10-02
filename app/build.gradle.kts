import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
    checkstyle
    jacoco
}

// N-09: the TMDB token lives only in local.properties (ignored by Git) or in the
// TMDB_TOKEN environment variable (CI) and reaches the code through BuildConfig.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.isFile) file.inputStream().use { load(it) }
}

fun localOrEnv(name: String): String? = localProperties.getProperty(name) ?: providers.environmentVariable(name).orNull

val tmdbToken = localOrEnv("TMDB_TOKEN").orEmpty()
val tmdbApiBaseUrl = localOrEnv("TMDB_API_BASE_URL") ?: "https://api.themoviedb.org/3/"
val tmdbImageBaseUrl = localOrEnv("TMDB_IMAGE_BASE_URL") ?: "https://image.tmdb.org/t/p/"

android {
    namespace = "ru.kinopolka"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "ru.kinopolka"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "TMDB_TOKEN", "\"$tmdbToken\"")
        buildConfigField("String", "TMDB_API_BASE_URL", "\"$tmdbApiBaseUrl\"")
        buildConfigField("String", "TMDB_IMAGE_BASE_URL", "\"$tmdbImageBaseUrl\"")
    }

    buildTypes {
        debug {
            // JaCoCo coverage of unit tests: ./gradlew createDebugUnitTestCoverageReport
            enableUnitTestCoverage = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Course project: the release APK is signed with the debug key so that it can be
            // installed on a device for the demo. A store release would use its own keystore.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    // Exported Room schemas are read by MigrationTestHelper. Robolectric sees only the merged
    // assets of the debug app, so the schemas are debug assets (release builds do not get them).
    sourceSets.getByName("debug").assets.directories.add("$projectDir/schemas")

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            // Robolectric (Android SDK 36) reads FileDescriptor internals, closed by default on JDK 17+.
            it.jvmArgs(
                "--add-exports=java.base/jdk.internal.access=ALL-UNNAMED",
                "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
            )
            // Robolectric loads Android classes in its own class loader.
            it.extensions.configure<JacocoTaskExtension> {
                isIncludeNoLocationClasses = true
                excludes = listOf("jdk.internal.*")
            }
        }
    }

    packaging {
        resources {
            excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "/META-INF/LICENSE*.md")
        }
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
    }
}

room {
    // exportSchema = true: every schema version is stored in the repository for migration tests.
    schemaDirectory("$projectDir/schemas")
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

checkstyle {
    toolVersion = libs.versions.checkstyle.get()
    configFile = rootProject.file("config/checkstyle/checkstyle.xml")
    maxWarnings = 0
}

// Java code style: ./gradlew checkstyle
val checkstyleTask = tasks.register<Checkstyle>("checkstyle") {
    group = "verification"
    description = "Checks the Java sources with Checkstyle."
    source("src")
    include("**/*.java")
    classpath = files()
    reports {
        html.required.set(true)
        xml.required.set(false)
    }
}

// Coverage rule (docs/PLAN.md, section 8): at least 60% of the lines in core/data and core/database.
val coverageClasses = fileTree(layout.buildDirectory.dir("intermediates/javac/debug/compileDebugJavaWithJavac/classes")) {
    include("ru/kinopolka/core/data/**", "ru/kinopolka/core/database/**")
    exclude(
        "**/*_Impl*", "**/*_Factory*", "**/*_MembersInjector*", "**/*Hilt_*", "**/*_HiltModules*",
        "**/di/**", "**/BuildConfig*",
    )
}
val coverageData = layout.buildDirectory.file("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")

tasks.register<JacocoReport>("coverageReport") {
    group = "verification"
    description = "HTML coverage report of core/data and core/database."
    dependsOn("testDebugUnitTest")
    classDirectories.setFrom(coverageClasses)
    sourceDirectories.setFrom("src/main/java")
    executionData.setFrom(coverageData)
    reports {
        html.required.set(true)
        xml.required.set(false)
    }
}

tasks.register<JacocoCoverageVerification>("coverageVerify") {
    group = "verification"
    description = "Fails when less than 60% of the lines in core/data and core/database are covered."
    dependsOn("testDebugUnitTest")
    classDirectories.setFrom(coverageClasses)
    sourceDirectories.setFrom("src/main/java")
    executionData.setFrom(coverageData)
    violationRules {
        rule {
            limit {
                counter = "LINE"
                minimum = "0.60".toBigDecimal()
            }
        }
    }
}

tasks.named("check") { dependsOn(checkstyleTask) }

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
}

dependencies {
    implementation(libs.androidx.activity)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.material)

    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.guava)
    implementation(libs.guava)

    implementation(libs.hilt.android)
    annotationProcessor(libs.hilt.compiler)
    implementation(libs.androidx.hilt.work)
    annotationProcessor(libs.androidx.hilt.compiler)
    implementation(libs.androidx.work.runtime)

    implementation(libs.androidx.room.runtime)
    annotationProcessor(libs.androidx.room.compiler)

    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.gson)
    implementation(libs.glide)

    debugImplementation(libs.leakcanary.android)

    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.test.espresso.core)
    testImplementation(libs.androidx.test.espresso.contrib)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.work.testing)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.retrofit.mock)
    testImplementation(libs.hilt.android.testing)
    testAnnotationProcessor(libs.hilt.compiler)
    debugImplementation(libs.androidx.fragment.testing)
}

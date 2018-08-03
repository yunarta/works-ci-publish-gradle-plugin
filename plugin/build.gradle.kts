import com.mobilesolutionworks.gradle.publish.PublishedDoc
import com.mobilesolutionworks.gradle.publish.worksPublication
import groovy.json.JsonSlurper
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.nio.file.*

plugins {
    kotlin("jvm") version "1.2.50"
    groovy
    `java-gradle-plugin`
    jacoco

    id("io.gitlab.arturbosch.detekt") version "1.0.0.RC7-2"
    id("org.jetbrains.dokka") version "0.9.17"
    id("com.gradle.plugin-publish") version "0.9.10"
    id("com.mobilesolutionworks.gradle.jacoco") version "1.0.0"
    id("com.mobilesolutionworks.gradle.publish") version "1.5.3"

}

worksPublication?.apply {
    javadoc = PublishedDoc.Kotlin
    module = file("module.yaml")
}

val kotlinVersion: String by rootProject.extra
val sourceSets: SourceSetContainer = java.sourceSets

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

jacoco {
    toolVersion = "0.8.1"
    reportsDir = file("$buildDir/reports")
}

worksJacoco {
    hasTestKit = true
}

detekt {
    version = "1.0.0.RC7-2"

    profile("main", Action {
        input = "src/main/kotlin"
        filters = ".*/resources/.*,.*/build/.*"
        config = file("default-detekt-config.yml")
        output = "$buildDir/reports/detekt"
        outputName = "detekt-report"
        baseline = "reports/baseline.xml"
    })
}

//val jacocoAnt by configurations.creating
val jacocoRuntime by configurations.creating
val pluginRuntime by configurations.creating

dependencies {
    jacocoRuntime("org.jacoco:org.jacoco.agent:0.8.1")

    testImplementation(gradleTestKit())
    testImplementation("junit:junit:4.12")
    testImplementation("org.mockito:mockito-core:2.19.0")
    testImplementation("com.nhaarman:mockito-kotlin:1.5.0")
    testImplementation("org.spockframework:spock-core:1.1-groovy-2.4") {
        exclude(group = "org.codehaus.groovy")
    }

    compileOnly(gradleApi())
    implementation(kotlin("stdlib-jdk8", kotlinVersion))
    implementation(kotlin("reflect", kotlinVersion))

    compileOnly("org.jetbrains.dokka:dokka-android-gradle-plugin:0.9.17")
    compileOnly("com.android.tools.build:gradle:3.1.2") {
        isTransitive = false
    }
    compileOnly("com.android.tools.build:builder-model:3.1.2") {
        isTransitive = false
    }
    compileOnly("com.android.tools.build:gradle-core:3.1.2") {
        isTransitive = false
    }

    pluginRuntime("org.jetbrains.dokka:dokka-android-gradle-plugin:0.9.17")
    pluginRuntime("com.android.tools.build:gradle:3.1.2")
    pluginRuntime("org.jetbrains.kotlin:kotlin-gradle-plugin:1.2.50")
}

gradlePlugin {
    (plugins) {
        "works-publish" {
            id = "com.mobilesolutionworks.gradle.publish"
            implementationClass = "com.mobilesolutionworks.gradle.publish.PublishPlugin"
        }
    }
}

pluginBundle {
    website = "https://github.com/yunarta/https://github.com/yunarta/works-ci-publish-gradle-plugin"
    vcsUrl = "https://github.com/yunarta/https://github.com/yunarta/works-ci-publish-gradle-plugin"
    description = "Reusable project publishing to be used in along Jenkins pipeline."
    tags = listOf("jacoco", "works")

    (plugins) {
        "works-publish" {
            id = "com.mobilesolutionworks.gradle.publish"
            displayName = "Reusable project publishing to be used in along Jenkins pipeline."
        }
    }
}

tasks.withType<PluginUnderTestMetadata> {
    pluginClasspath += configurations["pluginRuntime"].asFileTree
//    println("compileOnly")
//    configurations["compileOnly"].asFileTree.forEach {
//        println(" - $it")
//    }
    println("PluginUnderTestMetadata")
    pluginClasspath.forEach {
        println(" - $it")
    }
}

tasks.withType<Delete>().whenObjectAdded {
    if (name == "cleanTest") {
        delete(file("$buildDir/tmp/runTest"))
    }
}

tasks.withType<JacocoReport> {
    reports {
        xml.isEnabled = true
        html.isEnabled = true
    }

    // generated classes
    classDirectories = fileTree(mapOf(
            "dir" to "$buildDir/classes/java/main")
    ) + fileTree(mapOf(
            "dir" to "$buildDir/classes/kotlin/main")
    )

    // sources
    sourceDirectories = files(listOf("src/main/kotlin", "src/main/java", "/src/test/groovy"))
    executionData = fileTree(mapOf("dir" to project.rootDir.absolutePath, "include" to "**/build/jacoco/*.exec"))
}

tasks.create("automationCheck") {
    group = "automation"
    description = "Execute check"

    dependsOn("detektCheck")
}

tasks.withType<KotlinCompile> {
    group = "compilation"
    kotlinOptions.jvmTarget = "1.8"
}

val ignoreFailures: String? by rootProject.extra
val shouldIgnoreFailures = ignoreFailures?.toBoolean() ?: false

tasks.withType<Test> {
    maxParallelForks = Math.max(1, Runtime.getRuntime().availableProcessors().div(2))
    ignoreFailures = shouldIgnoreFailures

    doFirst {
        logger.quiet("Test with max $maxParallelForks parallel forks")
    }
}
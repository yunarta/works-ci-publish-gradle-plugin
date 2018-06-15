import com.mobilesolutionworks.gradle.publish.PublishedDoc
import com.mobilesolutionworks.gradle.publish.worksPublication

plugins {
    `java-library`
}

apply {
    plugin("works-publish")
}

group = "com.mobilesolutionworks"
version = "1.0-SNAPSHOT"

worksPublication?.apply {
    javadoc = PublishedDoc.Kotlin
    module = File("module.properties")
}

dependencies {
    compileOnly(gradleApi())
}
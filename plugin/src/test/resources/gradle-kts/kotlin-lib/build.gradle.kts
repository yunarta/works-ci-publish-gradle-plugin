import com.mobilesolutionworks.gradle.publish.PublishedDoc
import com.mobilesolutionworks.gradle.publish.worksPublication

plugins {
    `java-gradle-plugin`
    `java-library`
    id("works-publish")
}

group = "com.mobilesolutionworks"
version = "1.0-SNAPSHOT"

worksPublication?.apply {
    javadoc = PublishedDoc.Kotlin
    module = file("module.properties")
}

dependencies {
    compileOnly(gradleApi())
}
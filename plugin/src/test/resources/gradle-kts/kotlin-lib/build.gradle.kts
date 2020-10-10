import com.mobilesolutionworks.gradle.publish.Publication
import com.mobilesolutionworks.gradle.publish.PublishedDoc

plugins {
    `java-library`
}

apply {
    plugin("works-publish")
}

group = "com.mobilesolutionworks"
version = "1.0-SNAPSHOT"

val org.gradle.api.Project.`worksPublish`: com.mobilesolutionworks.gradle.publish.Publication get() =
    extensions.findByName("publication") as Publication

worksPublish.apply {
    javadoc = PublishedDoc.Kotlin
    module = file("module.properties")
}

dependencies {
    compileOnly(gradleApi())
}
import com.mobilesolutionworks.gradle.publish.PublishedDoc

plugins {
    `java-gradle-plugin`
    `java-library`
    id("works-publish")
}

group = "com.mobilesolutionworks"
version = "1.0-SNAPSHOT"

worksPublish {
    javadoc = PublishedDoc.Kotlin
    module = file("module.properties")
}

dependencies {
    compileOnly(gradleApi())
}
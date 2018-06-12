import com.mobilesolutionworks.gradle.publish.workPublication

plugins {
    `java-library`
}

apply {
    plugin("works-publish")
}

group = "com.mobilesolutionworks"
version = "1.0-SNAPSHOT"

//val publication = extensions.findByName("publication")
//println("publication = " + publication)
//{
    ////    javadoc =
////    module = File("module.properties")
//}

//
//version '1.0.0'
//
//publication {
//    javadoc = "kotlin"
//    includeTest = true
//}
//
//sourceSets {
//    main {
//        java.srcDirs += ['src/main/kotlin']
//    }
//}
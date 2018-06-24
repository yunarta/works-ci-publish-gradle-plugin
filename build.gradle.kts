buildscript {
    var kotlinVersion: String by extra
    kotlinVersion = "1.2.50"

    repositories {
        jcenter()
        google()
        mavenCentral()
        maven {
            url = java.net.URI("https://dl.bintray.com/mobilesolutionworks/release")
        }
        maven {
            url = java.net.URI("https://dl.bintray.com/mobilesolutionworks/snapshot")
        }
    }

    dependencies {
        classpath("com.github.ben-manes:gradle-versions-plugin:0.19.0")
        classpath("com.mobilesolutionworks:works-publish:+")
    }
}

allprojects {
    repositories {
        jcenter()
        google()
        mavenCentral()
    }
}

subprojects {
    apply {
        plugin("com.github.ben-manes.versions")
    }
}

tasks.create("jacocoRootReport") {
    group = "automation"
    description = "Execute test with coverage"

    dependsOn(":works-publish:jacocoCoverageTest")
}

tasks.create("clean", Delete::class.java) {
    delete(rootProject.buildDir)
}
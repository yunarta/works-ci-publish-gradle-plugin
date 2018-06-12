buildscript {
    var kotlinVersion: String by extra
    kotlinVersion = "1.2.41"

    repositories {
        jcenter()
        google()
        mavenCentral()
    }

    dependencies {
        classpath(kotlin("gradle-plugin", kotlinVersion))
    }
}

allprojects {
    repositories {
        jcenter()
        google()
        mavenCentral()
    }
}


tasks.create("jacocoRootReport") {
    group = "automation"
    description = "Execute test with coverage"

    dependsOn(":works-publish:createJacocoTestReport")
}

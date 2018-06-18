# CI Publish - Gradle Plugin

[![Build Status](http://jenkins.mobilesolutionworks.com:8080/job/github/job/yunarta/job/works-ci-publish-gradle-plugin/job/master/badge/icon)](http://jenkins.mobilesolutionworks.com:8080/job/github/job/yunarta/job/works-ci-publish-gradle-plugin/job/master/)
[![codecov](https://codecov.io/gh/yunarta/works-ci-publish-gradle-plugin/branch/master/graph/badge.svg)](https://codecov.io/gh/yunarta/works-ci-publish-gradle-plugin)

Reusable project publishing to be used in along Jenkins pipeline.

What is it for:
- Minimum configuration for each project
- Compile and generate the necessary javadoc, source and binary for release
- Publish to repository with Jenkins pipeline

Why:
- Manually publishing may lead to many things from version collision, untested changes and many more
- Conventional gradle configuration may check for credential even publication is not intended
- Credential are not centralized

Usage:
- Make sure group and version is there as for POM requirement
- As for now a Java-Kotlin project must choose which Javadoc they wanted to use  

## Installation

```groovy
buildscript {
    repositories {
        maven {
            url = "https://dl.bintray.com/mobilesolutionworks/release"
        }
    }
    
    dependencies {
        classpath("com.mobilesolutionworks:works-publish:1.0.5")
    }    
}
```

## Configure

module **build.gradle**
```groovy
apply plugin: 'works-publish'

group 'com.mobilesolutionworks'
version '1.0.0'
            
publication {
    javadoc = "java|kotlin"
    includeTest = true|false
    module = file('module.properties')
}

```

module **build.gradle.kts**
```kotlin
import com.mobilesolutionworks.gradle.publish.worksPublication

apply {
    plugin("works-publish")
}

group = "com.mobilesolutionworks"
version = "1.0.0"
            
worksPublication?.apply {
    javadoc = PublishedDoc.Java|PublishedDoc.Kotlin
    includeTest = true|false
    // take note that using File() will point the module to project root instead   
    module = file("module.properties")
}
```
Parameter
- javadoc: Define how the publish plugin should generated the JavaDoc.
- includeTest: When set to true, test configuration dependencies will be included in output.
- module: When provided with properties file containing group and version, the provided value will replace the project 
after Gradle evaluation. 

## Usage

Execute:
```gradle worksGeneratePublication```

Once the execution is finished, you will find all the output artifact in your build directory libs.
This usually will consists of:
- POM file
- JAR or AAR binary
- Archived source
- Archived javadoc
- MD5 checksum of JAR/AAR content and of POM file

All the output are following the Maven 2 format in such
- **POM** - group-artifact-version.pom
- **POM Checksum** - group-artifact-version-pom.md5
- **JAR Artifact** - group-artifact-version.jar
- **AAR Artifact** - group-artifact-version.aar
- **Artifact Checksum** - group-artifact-version.md5
- **Javadoc** - group-artifact-version-javadoc.jar
- **Source** - group-artifact-version-source.jar

With this format, uploading to repository will be very easy especially with Jenkins CI

## Continuous Integration

The checksum output of the POM and Artifact can used to compare whether there are difference between one that you 
stored in repository with the one that the CI just created. This way you may prevent uploading the artifact again
and may prevent downstream build.

Currently the MD5 checksum is still experimental.
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

Configure:
```groovy
buildscript {
    dependencies {
        classpath 'com.mobilesolutionworks:works-publish:1.0.1'
    }
}

apply plugin: 'works-publish'

group 'com.mobilesolutionworks'
version '1.0.0'
            
publication {
    javadoc = "java|kotlin"
    includeTest = true|false
    module = file('module.properties')
}

```

Parameter
- javadoc: Define how the publish plugin should generated the JavaDoc.
- includeTest: When set to true, test configuration dependencies will be included in output.
- module: When provided with properties file containing group and version, the provided value will replace the project after Gradle evaluation. 

Execute:
```gradle worksGeneratePublication```


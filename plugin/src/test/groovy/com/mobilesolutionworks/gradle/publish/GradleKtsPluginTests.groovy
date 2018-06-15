package com.mobilesolutionworks.gradle.publish

import org.gradle.internal.impldep.org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.BuildResult
import org.junit.Before

import java.util.jar.JarFile

class GradleKtsPluginTests extends TestKitBaseSpecification {

    @Before
    static def configure() {
        def loader = TestSuite.class.classLoader

        def resource = loader.getResource("gradle-kts")
        FileUtils.copyDirectory(new File(resource.file), testDir.root)
    }

    def tasks = [
            "worksGeneratePublication"
    ]

    def "test gradle kts"() {
        when:
        def prefix = "kotlin-lib"
        def performTasks = tasks.collect { String.valueOf(":$prefix:$it") }
        BuildResult result = execute(performTasks)

        then:
        verifySuccess(result, performTasks)
        verifyStructure(prefix)
    }


    def verifyStructure(prefix, kotlin = false) {
        return {
            def libs = new File(testDir.root, "${prefix}/build/libs")

            def javadoc = new File(libs, "${prefix}-1.0.0-javadoc.jar")
            def sources = new File(libs, "${prefix}-1.0.0-sources.jar")
            def pom = new File(libs, "${prefix}-1.0.0.pom")
            def aar = new File(libs, "${prefix}-1.0.0.aar")
            def jar = new File(libs, "${prefix}-1.0.0.jar")

            pom.exists()
            sources.exists()
            javadoc.exists()
            aar.exists() || jar.exists()

            verifySource(sources, kotlin)
        }
    }

    def verifySource(File sources, kotlin = false) {
        def file = new JarFile(sources)
        def contents = file.entries().toList().collect {
            it.name
        }

        contents.contains("com/test/ExampleInJava.java")
        if (kotlin) {
            contents.contains("com/test/ExampleInKotlin.kt")
        }
    }
}

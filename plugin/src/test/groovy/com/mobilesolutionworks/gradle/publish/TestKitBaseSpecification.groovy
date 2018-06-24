package com.mobilesolutionworks.gradle.publish


import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.BeforeClass
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import java.nio.file.Paths

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

abstract class TestKitBaseSpecification extends Specification {

    private static tempDir = Paths.get("build", "tmp", "runTest").toFile()

    public static TemporaryFolder testDir = new TemporaryFolder(tempDir)

    private static File buildGradle

    @BeforeClass
    static def setupFolder() {
        tempDir.mkdirs()
        testDir.create()

        buildGradle = testDir.newFile("build.gradle")
    }

    GradleRunner gradleRunner

    @Before
    def configureRootGradle() {
        def loader = TestSuite.class.classLoader
        def className = getClass().name

        loader.getResourceAsStream("javaagent-for-testkit.properties")?.withStream {
            def properties = new Properties()
            properties.load(it)

            def agentPath = properties.getProperty("agentPath")
            def outputDir = properties.getProperty("outputDir")

            def execFile = new File(outputDir, "${className}.exec")
            def agentString = "org.gradle.jvmargs=-javaagent\\:${agentPath}\\=destfile\\=${execFile.absolutePath}"

            new File(testDir.root, "gradle.properties").write(agentString)
        }

        buildGradle.write("""
            |buildscript {
            |    repositories {
            |        google()
            |        jcenter()
            |    }
            |
            |    dependencies {
            |        // classpath 'com.android.tools.build:gradle:3.1.2'
            |        // classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:1.2.50'
            |        // classpath 'org.jetbrains.dokka:dokka-android-gradle-plugin:0.9.17'
            |    }
            |}
            |
            |allprojects {
            |   repositories {
            |       google()
            |       jcenter()
            |   }
            |}
        """.stripMargin('|'))

        gradleRunner = GradleRunner.create()
                .withProjectDir(testDir.root)
                .withPluginClasspath()
                .forwardOutput()
    }

    def execute(String... arguments) {
        execute(arguments.toList())
    }

    def execute(List<String> arguments) {
        def execute = new ArrayList(arguments) + "--stacktrace"
        gradleRunner.withArguments(execute).build()
    }

    def verifySuccess(BuildResult result, List<String> arguments) {
        return arguments.collect {
            result.task(it).outcome == SUCCESS
        }.inject(true) {
            initial, next -> initial && next
        }
    }
}

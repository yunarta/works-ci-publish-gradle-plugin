package com.mobilesolutionworks.gradle.publish

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

abstract class TestKitBaseSpecification extends Specification {

    @ClassRule
    public
    static TemporaryFolder testDir = new TemporaryFolder()

    private static File gradleProperties
    private static File buildGradle

    @BeforeClass
    static def setupFolder() {
        testDir.create()

        gradleProperties = testDir.newFile("gradle.properties")
        buildGradle = testDir.newFile("build.gradle")
    }

    @AfterClass
    static def deleteTestDir() {
        testDir.delete()
    }

    GradleRunner gradleRunner

    @Before
    def configureRootGradle() {
        def loader = TestSuite.class.classLoader
        def pluginClasspathResource = loader.getResource "plugin-classpath.txt"
        if (pluginClasspathResource == null) {
            throw new IllegalStateException("Did not find plugin classpath resource, run `testClasses` build task.")
        }

        def pluginClasspath = pluginClasspathResource.readLines().collect { new File(it) }
        def classpathString = pluginClasspath
                .collect { it.absolutePath.replace('\\', '\\\\') } // escape backslashes in Windows paths
                .collect { "'$it'" }
                .join(", ")

        def agent = loader.getResource("gradle.properties").text + File.pathSeparatorChar + "${getClass().name}.exec"
        agent = agent.replace('\\', '\\\\')

        gradleProperties.write("""${agent}
                                 |org.gradle.daemon=true""".stripMargin("|"))

        buildGradle.write("""
            |buildscript {
            |    repositories {
            |        google()
            |        jcenter()
            |    }
            |
            |    dependencies {
            |        classpath 'com.android.tools.build:gradle:3.1.2'
            |        classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:1.2.41'
            |        classpath 'org.jetbrains.dokka:dokka-android-gradle-plugin:0.9.17'
            |        classpath files($classpathString)
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
                .withGradleVersion("4.8")
                .withPluginClasspath(pluginClasspath)
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

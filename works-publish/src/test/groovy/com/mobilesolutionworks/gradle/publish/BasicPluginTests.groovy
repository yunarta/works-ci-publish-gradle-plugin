package com.mobilesolutionworks.gradle.publish

import org.gradle.internal.impldep.org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.BuildResult
import org.junit.Before

import java.util.jar.JarFile

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class BasicPluginTests extends TestKitBaseSpecification {

    @Before
    static def configure() {
        def loader = TestSuite.class.classLoader

        def resource = loader.getResource("basic")
        FileUtils.copyDirectory(new File(resource.file), testDir.root)
    }

    def tasks = [
            "worksArchiveDocumentation",
            "worksGenerateAssembly",
            "worksArchiveSources",
            "worksGeneratePom",
            "worksGeneratePublication"
    ]

    def "test java library project"() {
        when:
        def prefix = "java-lib"
        def performTasks = tasks.collect { String.valueOf(":$prefix:$it") }
        BuildResult result = execute(performTasks)

        then:
        verifySuccess(result, performTasks)
        verifyStructure(prefix)
    }

    def "test kotlin library project"() {
        when:
        def prefix = "kotlin-lib"
        def performTasks = tasks.collect { String.valueOf(":$prefix:$it") }
        BuildResult result = execute(performTasks)

        then:
        result.task(":${prefix}:dokka").outcome == SUCCESS
        verifySuccess(result, performTasks)
        verifyStructure(prefix, true)
    }

    def "test android java library project"() {
        when:
        def prefix = "android-java-lib"
        def performTasks = tasks.collect { String.valueOf(":$prefix:$it") }
        BuildResult result = execute(performTasks)

        then:
        verifySuccess(result, performTasks)
        verifyStructure(prefix)
    }

    def "test android kotlin library project"() {
        when:
        def prefix = "android-kotlin-lib"
        def performTasks = tasks.collect { String.valueOf(":$prefix:$it") }
        BuildResult result = execute(performTasks)

        then:
        result.task(":${prefix}:dokka").outcome == SUCCESS
        verifySuccess(result, performTasks)
        verifyStructure(prefix, true)
    }

    def "test android kotlin app project"() {
        when:
        BuildResult result = execute("clean", "worksGeneratePublication")

        then:
        result.task(":java-lib:worksGeneratePublication").outcome == SUCCESS
        result.task(":kotlin-lib:worksGeneratePublication").outcome == SUCCESS
        result.task(":android-java-lib:worksGeneratePublication").outcome == SUCCESS
        result.task(":android-kotlin-lib:worksGeneratePublication").outcome == SUCCESS

        result.task(":android-kotlin-app:worksGeneratePublication") == null
    }

    def "test module"() {
        when:
        new File(testDir.root, "module-test/module.properties").write("""
        |group=com.mobilesolutionworks
        |version=3.0.0""".stripMargin().trim())
        execute("clean", ":module-test:worksGeneratePublication")

        then:
        verifyPom("module-test", [group: "com.mobilesolutionworks", version: "3.0.0"])
    }

    def "test module - file removed"() {
        when:
        new File(testDir.root, "module-test/module.properties").delete()
        execute("clean", ":module-test:worksGeneratePublication")

        then:
        verifyPom("module-test", [group: "com.default", version: "1.0.0"])
    }

    def "test module - no group key"() {
        when:
        new File(testDir.root, "module-test/module.properties").write("""
        |wrongGroup=com.mobilesolutionworks
        |version=3.0.0""".stripMargin().trim())

        execute("clean", ":module-test:worksGeneratePublication")

        then:
        verifyPom("module-test", [group: "com.default", version: "1.0.0"])
    }

    def "test module - no version key"() {
        when:
        new File(testDir.root, "module-test/module.properties").write("""
        |group=com.mobilesolutionworks
        |wrongVersion=3.0.0""".stripMargin().trim())

        execute("clean", ":module-test:worksGeneratePublication")

        then:
        verifyPom("module-test", [group: "com.default", version: "1.0.0"])
    }

    def "test pom dependencies"() {
        when:
        def prefix = "java-lib"
        def performTasks = tasks.collect { String.valueOf(":$prefix:$it") }
        BuildResult result = execute(performTasks)

        then:
        verifyDependencies("java-lib")
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

    def verifyPom(prefix, Map module) {
        def libs = new File(testDir.root, "${prefix}/build/libs")
        def pom = new File(libs, "module-test-${module.version}.pom")
        pom.exists()

        def pomXml = new XmlSlurper().parseText(pom.text)
        module.group == pomXml.groupId.text()
        module.version == pomXml.version.text()
    }

    def verifyDependencies(prefix) {
        def libs = new File(testDir.root, "${prefix}/build/libs")
        def pom = new File(libs, "${prefix}-1.0.0.pom")
        pom.exists()

        def pomXml = new XmlSlurper().parseText(pom.text)
        def dependencies = pomXml.dependencies.dependency.collect {
            "${it.scope.text()} ${it.groupId.text()}:${it.artifactId.text()}:${it.version.text()}".toString()
        }

        dependencies.contains("test junit:junit:4.12")
        dependencies.contains("compile io.reactivex.rxjava2:rxjava:2.1.12")
        dependencies.contains("runtime io.reactivex.rxjava2:rxandroid:2.0.2")
        dependencies.contains("provided com.parse.bolts:bolts-tasks:1.4.0")
    }
}

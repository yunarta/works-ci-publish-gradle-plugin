package com.mobilesolutionworks.gradle.publish

import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.logging.LogLevel
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.plugins.MavenPlugin
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.internal.logging.text.StyledTextOutput
import org.gradle.internal.logging.text.StyledTextOutputFactory
import org.gradle.jvm.tasks.Jar
import java.util.*

interface ProjectConfigurator {

    fun configureSource(project: Project, jar: Jar)

    fun configureJavadoc(project: Project, javadoc: Javadoc)
}

class JavaLibConfigurator : ProjectConfigurator {

    override fun configureSource(project: Project, jar: Jar) {
        val java = project.convention.getPlugin(JavaPluginConvention::class.java)
        java.sourceSets.getByName("main").java.srcDirs.forEach {
            jar.from(it)
        }
    }

    override fun configureJavadoc(project: Project, javadoc: Javadoc) {
    }
}

class AndroidLibConfiguratorImpl(private val extension: LibraryExtension) : ProjectConfigurator {

    override fun configureSource(project: Project, jar: Jar) {
        extension.sourceSets.getByName("main").java.srcDirs.forEach {
            jar.from(it)
        }
    }

    override fun configureJavadoc(project: Project, javadoc: Javadoc) {
        javadoc.source = project.makeFileTree(extension.sourceSets.getAt("main").java.srcDirs)
        javadoc.classpath += project.makeFileTree(extension.bootClasspath)

        javadoc.classpath += project.makeFileTree(extension.libraryVariants.flatMap { variant ->
            @Suppress("DEPRECATION")
            variant.javaCompile.classpath.files
        })
    }
}

internal class PublishOptions {

    var isAndroidLibrary = false
    var skipConfiguration = false

    var configurator: ProjectConfigurator = JavaLibConfigurator()
}

/**
 * Plugin implementation defined for this Gradle plugin.
 */
class PublishPlugin : Plugin<Project> {

    private val opts = PublishOptions()

    private var log: LogWriter = LogWriter { }

    private lateinit var publication: Publication

    /**
     * Apply the plugin to specified project.
     *
     * @param project Gradle project.
     */
    override fun apply(project: Project) {
        with(project) {
            publication = extensions.create("publication", Publication::class.java)
            createLogWriter(this)

            project.afterEvaluate {
                createOptions(this, extensions)
                if (opts.skipConfiguration) return@afterEvaluate

                publication.module?.let {
                    if (!it.exists()) {
                        logWarn("module is provided, file is not found")
                    } else {
                        val module = Properties()
                        module.load(it.reader())

                        val group = module["group"]
                        val version = module["version"]
                        when {
                            group != null && version != null -> {
                                project.group = group
                                project.version = version
                            }
                            else -> {
                                logWarn("module ${it.name} is provided, but group and version is not defined")
                            }
                        }
                    }
                }

                plugins.apply(MavenPlugin::class.java)
                plugins.apply(MavenPublishPlugin::class.java)
                createPOM(project, extensions)

                generateDocumentationTask(this)
                generateArchiveSourceTask(this)
                generateArchiveTask(this)
                generatePomTask(this)
                generatePublicationTask(this)
            }
        }
    }

    internal fun createPOM(project: Project, extensions: ExtensionContainer) {
        val it = extensions.findByName("publishing")
        if (it != null && it is PublishingExtension) {
            it.publications {
                it.create("workPublish", MavenPublication::class.java) { maven ->
                    maven.artifactId = project.name
                    maven.version = project.version.toString()

                    if (opts.isAndroidLibrary) {
                        maven.artifact("${project.buildDir}/libs/${project.name}-${project.version}.aar")
                    } else {
                        maven.artifact("${project.buildDir}/libs/${project.name}-${project.version}.jar")
                    }

                    maven.pom.withXml {
                        val root = it.asNode()

                        val license = root.appendNode("licenses").appendNode("license")
                        license.appendNode("name", "The Apache Software License, Version 2.0")
                        license.appendNode("url", "http://www.apache.org/licenses/LICENSE-2.0.txt")
                        license.appendNode("distribution", "repo")

                        val dependenciesNode = root.appendNode("dependencies")
                        val dependencies = mutableMapOf<String, DependencyData>()

                        val transforms = mutableListOf(
                                DependencyTransform.TestImplementation,
                                DependencyTransform.Implementation,
                                DependencyTransform.Api,
                                DependencyTransform.CompileOnly
                        )
                        if (!publication.includeTest) {
                            transforms.removeAt(0)
                        }
                        transforms.forEach { transform ->
                            project.configurations.getAt(transform.configuration).allDependencies.forEach {
                                val key = "${it.group.toString()}:${it.name}:${it.version}"
                                dependencies[key] = DependencyData(
                                        it.group,
                                        it.name,
                                        it.version,
                                        transform.scope
                                )
                            }
                        }

                        project.logger.quiet("""Project Object Model
                                   |====================
                                   |${project.group}:${maven.artifactId}:${project.version}
                                   |Deps""".trimMargin("|"))
                        dependencies.values.forEach {
                            project.logger.quiet("""  ${it.scope}: ${it.group}:${it.artifact}:${it.version}""")

                            val dependencyNode = dependenciesNode.appendNode("dependency")
                            dependencyNode.appendNode("groupId", it.group)
                            dependencyNode.appendNode("artifactId", it.artifact)
                            dependencyNode.appendNode("version", it.version)
                            dependencyNode.appendNode("scope", it.scope)
                        }

                        project.logger.quiet("")
                    }
                }
            }
        }
    }

    private fun createOptions(project: Project, extensions: ExtensionContainer) {
        fun configureAndroidOptions() {
            val extension = extensions.findByName("android")
            if (extension != null) {
                if (extension is LibraryExtension) {
                    opts.configurator = AndroidLibConfiguratorImpl(extension)
                    opts.isAndroidLibrary = true
                } else {
                    opts.skipConfiguration = true
                }
            }

//            opts.skipConfiguration = extensions.has("android") && !opts.isAndroidLibrary
        }

        fun configureKotlinOptions() {
            if (extensions.has("kotlin")) {
                if (project.tasks.findByPath("dokka") == null) {
                    logWarn("""No documentation will be generated for Kotlin project without Dokka plugin
                                       |You can check https://github.com/Kotlin/dokka for instruction on adding Dokka
                            """.trimMargin())
                }
            }
        }

        configureAndroidOptions()
        configureKotlinOptions()
    }

    private fun generateDocumentationTask(project: Project) {
        with(project) {
            val generateJavadoc = tasks.create("worksGenerateJavadoc", Javadoc::class.java) { javadoc ->
                javadoc.optionsFile.writeText("""-Xdoclint:none
                                    |-quiet""")
                javadoc.exclude("**/*.kt")

                opts.configurator.configureJavadoc(project, javadoc)
            }

            tasks.create("worksArchiveDocumentation", Jar::class.java) { jar ->
                val task = tasks.findByPath("dokka")?.takeIf {
                    extensions.has("kotlin") && publication.javadoc == PublishedDoc.Kotlin
                } ?: generateJavadoc

                jar.classifier = "javadoc"
                jar.dependsOn(task.name)
                jar.from(task.outputs)
            }
        }
    }

    private fun generateArchiveSourceTask(project: Project) {
        with(project) {
            tasks.create("worksArchiveSources", Jar::class.java) { jar ->
                jar.classifier = "sources"
                opts.configurator.configureSource(project, jar)
            }
        }
    }

    private fun generateArchiveTask(project: Project) {
        with(project) {
            when {
                opts.isAndroidLibrary -> {
                    tasks.create("worksGenerateAssembly", Copy::class.java) { copy ->
                        copy.dependsOn("assembleRelease")
                        copy.from("$buildDir/outputs/aar")
                        copy.into("$buildDir/libs")
                        copy.include("*-release.aar")
                        copy.rename("(.*)-release.aar", "${project.name}-${project.version}.aar")
                    }
                }

                else -> {
                    tasks.create("worksGenerateAssembly") {
                        it.dependsOn("assemble")
                    }
                }
            }
        }
    }

    private fun generatePomTask(project: Project) {
        with(project) {
            tasks.create("worksGeneratePom", Copy::class.java) { copy ->
                copy.dependsOn("generatePomFileForWorkPublishPublication")
                copy.from("$buildDir/publications/WorkPublish")
                copy.into("$buildDir/libs/")
                copy.rename("(.*)-(.*).xml", "${project.name}-${project.version}.pom")
            }
        }
    }

    private fun generatePublicationTask(project: Project) {
        with(project) {
            tasks.create("worksCalculateChecksum") {
                it.dependsOn(
                        "worksGenerateAssembly",
                        "worksArchiveSources",
                        "worksArchiveDocumentation",
                        "worksGeneratePom"
                )

                it.doLast {
                    val root = file("${project.buildDir}/checksum/binary")
                    val binary = if (opts.isAndroidLibrary) {
                        file("${project.buildDir}/libs/${project.name}-${project.version}.aar")
                    } else {
                        file("${project.buildDir}/libs/${project.name}-${project.version}.jar")
                    }

                    val hash = md5zip(binary, root) {
                        if (it.name == "classes.jar") {
                            md5zip(it, file("${project.buildDir}/checksum/jar"))
                        } else {
                            null
                        }
                    }.hexString()
                    file("${project.buildDir}/libs/${project.name}-${project.version}.md5").writeText(hash)

                    val pom = file("${project.buildDir}/libs/${project.name}-${project.version}.pom")
                    file("${project.buildDir}/libs/${project.name}-${project.version}-pom.md5")
                            .writeText(pom.md5().hexString())
                }
            }

            tasks.create("worksGeneratePublication") {
                it.dependsOn("worksCalculateChecksum")
                it.group = "publishing"
            }
        }
    }

    internal fun logWarn(text: String) {
        log.warn(text)
    }

    internal fun createLogWriter(project: Project) {
        log = if (project is ProjectInternal) {
            val factory = project.services.get(StyledTextOutputFactory::class.java)
            val output = factory.create("works-warning", LogLevel.WARN)

            LogWriter { text -> output.withStyle(StyledTextOutput.Style.Failure).println(text) }
        } else {
            LogWriter { text -> project.logger.quiet(text) }
        }
    }
}
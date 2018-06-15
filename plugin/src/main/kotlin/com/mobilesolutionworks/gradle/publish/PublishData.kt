package com.mobilesolutionworks.gradle.publish

import java.io.File

/**
 * Enum for javadoc type.
 */
enum class PublishedDoc {

    /**
     * When "kotlin" is being provided, the process will use Dokka (https://github.com/Kotlin/dokka).
     */
    Java,

    /**
     * When "java" is being provided, the process will use Javadoc.
     */
    Kotlin
}

/**
 * Extension class for plugin user to modify the behavior.
 */
open class Publication {

    /**
     * Javadoc generation method.
     *
     * When "kotlin" is being provided, the process will use Dokka (https://github.com/Kotlin/dokka).
     * Where as when "java" is being provided, the process will use Javadoc.
     *
     * Please note that Javadoc is unable to process Kotlin files for documentation.
     */
    var javadoc: PublishedDoc = PublishedDoc.Java

    /**
     * Include test dependency into POM.
     */
    var includeTest: Boolean = false

    /**
     * Module file to use to replace project group and version.
     *
     * The module file is in Java properties format and should contains group and version.
     */
    var module: File? = null
}
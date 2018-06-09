package com.mobilesolutionworks.gradle.publish;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.jvm.tasks.Jar;

public interface JavadocArchiveConfigurator {

    void call(Project project, Jar jar, Task javadoc);
}

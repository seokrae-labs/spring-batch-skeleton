package buildlogic

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ProjectDependency

class BatchModuleRules {

    private static final Map<String, List<String>> ALLOWED_PROJECT_DEPENDENCIES = [
            core    : [],
            db      : [],
            job     : ['batch-core', 'batch-db-'],
            launcher: ['batch-job-'],
    ]

    private static final Map<String, String> RULE_TEXT = [
            core    : 'batch-core depends on no other module',
            db      : 'batch-db-* modules depend on no other module',
            job     : 'batch-job-* modules may depend only on batch-core and batch-db-*',
            launcher: 'batch-launcher-* modules may depend only on batch-job-*',
    ]

    static void enforce(Project project, String role) {
        project.afterEvaluate {
            List<String> problems = []
            checkName(project, role, problems)
            checkDependencies(project, role, problems)
            if (!problems.isEmpty()) {
                throw new GradleException(
                        "Module rules violated in ${project.path}:\n" +
                                problems.collect { "  - ${it}" }.join('\n') +
                                "\nRule: ${RULE_TEXT[role]}. Spring Boot is allowed only in batch-launcher-* modules." +
                                "\nSee README, section \"모듈 이름 규칙\".")
            }
        }
    }

    private static void checkName(Project project, String role, List<String> problems) {
        boolean valid = role == 'core'
                ? project.name == 'batch-core'
                : project.name ==~ /batch-${role}-[a-z0-9]+(-[a-z0-9]+)*/
        if (!valid) {
            String expected = role == 'core' ? 'batch-core' : "batch-${role}-<name>"
            problems << "module '${project.name}' applies batch.${role} but is not named ${expected}"
        }
    }

    private static void checkDependencies(Project project, String role, List<String> problems) {
        List<String> allowed = ALLOWED_PROJECT_DEPENDENCIES[role]
        project.configurations.each { configuration ->
            configuration.dependencies.withType(ProjectDependency).each { dependency ->
                String target = dependency.path.tokenize(':').last()
                if (target == project.name) {
                    return
                }
                boolean permitted = allowed.any { prefix ->
                    prefix.endsWith('-') ? target.startsWith(prefix) : target == prefix
                }
                if (!permitted) {
                    problems << "${project.name} -> ${target} (${configuration.name})"
                }
            }
            if (role != 'launcher') {
                configuration.dependencies.withType(ExternalModuleDependency).each { dependency ->
                    if (dependency.group == 'org.springframework.boot') {
                        problems << "${project.name} -> ${dependency.group}:${dependency.name} (${configuration.name})"
                    }
                }
            }
        }
    }
}

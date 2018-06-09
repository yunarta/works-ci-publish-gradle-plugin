buildCount = env.DEFAULT_HISTORY_COUNT ?: "5"

pipeline {
    agent {
        node {
            label 'android'
        }
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: buildCount))
        disableConcurrentBuilds()
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage("Build Test") {
            options {
                retry(2)
            }
            steps {
                sh './gradlew clean testWithCoverage -q'
                junit allowEmptyResults: true, testResults: '**/test-results/**/*.xml'
                jacoco execPattern: 'works-publish/build/jacoco/*.exec', classPattern: 'works-publish/build/classes/**/main', sourcePattern: 'works-publish/src/main/kotlin,works-publish/src/main/java'
            }
        }

        stage("Analyze") {
            steps {
                sh "./gradlew detektCheck -q"
                checkstyle canComputeNew: false, defaultEncoding: '', healthy: '', pattern: '**/detekt-report.xml', unHealthy: ''
            }
        }

        stage("Publish") {
            steps {
                codeCoverage()
            }
        }

        stage("Release") {
            when { branch 'release/*' }
            steps {
                sh "./gradlew worksCreatePublication -q"
                publish()
            }
        }
    }
}

def publish() {
    Map module = readJson file: 'works-publish/module.json'
    module["repo"] = "libs-gradle"
    module["path"] = "works-publish"
}

def codeCoverage() {
    withCredentials([[$class: 'StringBinding', credentialsId: "codecov-token", variable: "CODECOV_TOKEN"]]) {
        sh "curl -s https://codecov.io/bash | bash -s - -f works-publish/build/reports/createJacocoTestReport/createJacocoTestReport.xml"
    }
}

def artifactoryPublish(Map config) {
    def group = config.group
    def artifact = config.artifact
    def version = config.version

    def path = config.root

    def server = Artifactory.server "REPO"
    def result = server.upload spec: """{
                          "files": [
                             {
                              "pattern": "${path}/build/libs/(${artifact}-${version}*)",
                              "target": "${repo}/${group}/${artifact}/${version}/{1}"
                             }
                          ]
                        }"""
    server.publishBuildInfo result
}
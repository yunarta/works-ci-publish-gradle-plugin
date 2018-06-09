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
        retry(2)
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh './gradlew assemble -q'
            }
        }

        stage("Test") {
            steps {
                sh './gradlew testWithCoverage -q'
                junit allowEmptyResults: true, testResults: '**/test-results/**/*.xml'
                jacoco execPattern: '**/build/jacoco/*.exec', classPattern: '**/build/classes/**/main', sourcePattern: '**/src/main/**'
            }
        }

        stage("Analyze") {
            steps {
                sh "./gradlew detektCheck -q"
                checkstyle canComputeNew: false, defaultEncoding: '', healthy: '', pattern: '**/detekt-report.xml', unHealthy: ''
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
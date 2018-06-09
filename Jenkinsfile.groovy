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

        stage("Build") {
            parallel {
                stage("Test & Analyze") {
                    when { branch '^(?!(release\\/*).*$).*' }
                    options {
                        retry(2)
                    }
                    steps {
                        echo "Build for test and analyze"

                        sh './gradlew clean testWithCoverage -PignoreFailures=true -q'
                        sh "./gradlew detektCheck -q"
                    }
                }

                stage("Release") {
                    options {
                        retry(2)
                    }
                    when { branch 'release/*' }
                    steps {
                        echo "Build for release"

                        sh './gradlew clean test worksCreatePublication -PignoreFailures=false -q'
                    }
                }
            }
        }
        stage("Publish") {
            parallel {
                stage("Test & Analyze") {
                    when { branch '^(?!(release\\/*).*$).*' }
                    steps {
                        echo "Publishing test and analyze result"

                        junit allowEmptyResults: true, testResults: '**/test-results/**/*.xml'
                        jacoco execPattern: 'works-publish/build/jacoco/*.exec', classPattern: 'works-publish/build/classes/**/main', sourcePattern: 'works-publish/src/main/kotlin,works-publish/src/main/java'
                        checkstyle canComputeNew: false, defaultEncoding: '', healthy: '', pattern: '**/detekt-report.xml', unHealthy: ''
                        codeCoverage()
                    }
                }

                stage("Release") {
                    when { branch 'release/*' }
                    steps {
                        echo "Publishing release"

                        publish()
                    }
                }
            }
        }
    }
}

def publish() {
    def who = env.JENKINS_WHO ?: "anon"
    if (who == "works") {
        bintrayPublish([
                credential: "mobilesolutionworks.jfrog.org",
                pkg       : readJSON(file: 'works-publish/module.json'),
                repo      : "mobilesolutionworks/release",
                src       : "works-publish/build/libs"
        ])
    } else {
        Map module = readJSON file: 'works-publish/module.json'
        module["repo"] = "libs-gradle"
        module["path"] = "works-publish"
    }
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
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

    environment {
        JFROGCLI = "${tool 'JfrogCLI'}"
    }

    stages {
        stage('Select') {
            parallel {
                stage('Checkout') {
                    when {
                        expression {
                            notIntegration()
                        }
                    }

                    steps {
                        checkout scm
                        seedReset()
                    }
                }

                stage('Integrate') {
                    when {
                        expression {
                            isIntegration()
                        }
                    }

                    steps {
                        echo "Execute integration"
                        stopUnless(isStartedBy("upstream"))
                    }
                }
            }
        }

        stage("Coverage, Analyze and Test") {
            when {
                expression {
                    notIntegration() && notRelease()
                }
            }

            options {
                retry(2)
            }

            steps {
                seedGrow("test")

                echo "Build for test and analyze"
                sh "./gradlew clean automationTest -PignoreFailures=${seedEval("test", [1: "true", "else": "false"])} -q"
                sh "./gradlew automationCheck -q"
            }
        }

        stage("Publish CAT") {
            when {
                expression {
                    notIntegration() && notRelease()
                }
            }

            steps {
                echo "Publishing test and analyze result"

                junit allowEmptyResults: true, testResults: '**/test-results/**/*.xml'
                jacoco execPattern: 'plugin/build/jacoco/*.exec', classPattern: 'plugin/build/classes/**/main', sourcePattern: 'plugin/src/main/kotlin,plugin/src/main/java'
                checkstyle canComputeNew: false, defaultEncoding: '', healthy: '', pattern: 'plugin/build/reports/detekt/detekt-report.xml', unHealthy: ''
                codeCoverage()
            }
        }

        stage("Build") {
            when {
                expression {
                    notIntegration() && notFeatureBranch()
                }
            }

            parallel {
                stage("Snapshot") {
                    when {
                        expression {
                            notRelease()
                        }
                    }

                    steps {
                        updateVersion()
                        sh './gradlew clean worksGeneratePublication'
                    }
                }

                stage("Release") {
                    when {
                        expression {
                            isRelease()
                        }
                    }

                    steps {
                        sh """./gradlew clean test worksGeneratePublication"""
                    }
                }
            }
        }

        stage("Compare") {
            when {
                expression {
                    notIntegration() && notFeatureBranch()
                }
            }


            parallel {
                stage("Snapshot") {
                    when {
                        expression {
                            notRelease()
                        }
                    }

                    steps {
                        echo "Compare snapshot"
                        compareArtifact("snapshot", "integrate/snapshot")
                    }
                }

                stage("Release") {
                    when {
                        expression {
                            isRelease()
                        }
                    }

                    steps {
                        echo "Compare release"
                        compareArtifact("release", "integrate/release")
                    }
                }
            }
        }

        stage("Publish") {
            when {
                expression {
                    doPublish()
                }
            }

            parallel {
                stage("Snapshot") {
                    when {
                        expression {
                            notIntegration() && notRelease()
                        }
                    }

                    steps {
                        echo "Publishing snapshot"
                        publish("snapshot")
                    }
                }

                stage("Release") {
                    when {
                        expression {
                            notIntegration() && isRelease()
                        }
                    }

                    steps {
                        echo "Publishing release"
                        publish("release")
                    }
                }
            }
        }
    }
}

def updateVersion() {
    def properties = readYaml(file: 'plugin/module.yaml')
    properties.version = properties.version + "-BUILD-${BUILD_NUMBER}"
    sh "rm plugin/module.yaml"
    writeYaml file: 'plugin/module.yaml', data: properties
}

def compareArtifact(String repo, String job) {
    bintrayDownloadMatches repository: "mobilesolutionworks/${repo}",
            packageInfo: readYaml(file: 'plugin/module.yaml'),
            credential: "mobilesolutionworks.jfrog.org"

    def same = bintrayCompare repository: "mobilesolutionworks/${repo}",
            packageInfo: readYaml(file: 'plugin/module.yaml'),
            credential: "mobilesolutionworks.jfrog.org",
            path: "plugin/build/libs"

    if (fileExists(".jenkins/notify")) {
        sh "rm .jenkins/notify"
    }

    if (same) {
        echo "Artifact output is identical, no integration needed"
    } else {
        writeFile file: ".jenkins/notify", text: job
    }
}

def doPublish() {
    return fileExists(".jenkins/notify")
}

def publish(String repo) {
    bintrayPublish([
            credential: "mobilesolutionworks.jfrog.org",
            pkg       : readYaml(file: 'plugin/module.yaml'),
            repo      : "mobilesolutionworks/${repo}",
            src       : "plugin/build/libs"
    ])
}

def codeCoverage() {
    withCredentials([[$class: 'StringBinding', credentialsId: "codecov-token", variable: "CODECOV_TOKEN"]]) {
        sh "curl -s https://codecov.io/bash | bash -s - -f plugin/build/reports/jacocoCoverageTest/jacocoCoverageTest.xml"
    }
}
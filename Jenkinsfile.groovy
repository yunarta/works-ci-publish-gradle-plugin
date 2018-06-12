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
                sh "./gradlew detektCheck -q"
                sh "./gradlew clean testWithCoverage -PignoreFailures=${seedEval("test", [1: "true", "else": "false"])} -q"
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
                jacoco execPattern: 'works-publish/build/jacoco/*.exec', classPattern: 'works-publish/build/classes/**/main', sourcePattern: 'works-publish/src/main/kotlin,works-publish/src/main/java'
                checkstyle canComputeNew: false, defaultEncoding: '', healthy: '', pattern: '**/detekt-report.xml', unHealthy: ''
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

def compareArtifact(String repo, String job) {
    bintrayDownload([
            dir       : ".compare",
            credential: "mobilesolutionworks.jfrog.org",
            pkg       : readJSON(file: 'works-publish/module.json'),
            repo      : "mobilesolutionworks/${repo}",
            src       : "works-publish/build/libs"
    ])

    def same = bintrayCompare([
            dir       : ".compare",
            credential: "mobilesolutionworks.jfrog.org",
            pkg       : readJSON(file: 'works-publish/module.json'),
            repo      : "mobilesolutionworks/${repo}",
            src       : "works-publish/build/libs"
    ])

    if (fileExists(".notify")) {
        sh "rm .notify"
    }

    if (same) {
        echo "Artifact output is identical, no integration needed"
    } else {
        writeFile file: ".notify", text: job
    }
}

def doPublish() {
    return fileExists(".notify")
}

def publish(String repo) {
    bintrayPublish([
            credential: "mobilesolutionworks.jfrog.org",
            pkg       : readJSON(file: 'works-publish/module.json'),
            repo      : "mobilesolutionworks/release",
            src       : "works-publish/build/libs"
    ])
}

def codeCoverage() {
    withCredentials([[$class: 'StringBinding', credentialsId: "codecov-token", variable: "CODECOV_TOKEN"]]) {
        sh "curl -s https://codecov.io/bash | bash -s - -f works-publish/build/reports/createJacocoTestReport/createJacocoTestReport.xml"
    }
}
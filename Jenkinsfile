pipeline {
    agent {
        label 'amd64&&docker&&kotlin'
    }

    options {
        ansiColor('xterm')
        buildDiscarder logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '15')
        timestamps()
        disableConcurrentBuilds()
    }

    tools {
        maven 'Latest Maven'
    }

    triggers {
        pollSCM '@hourly'
        cron '@daily'
    }

    stages {
        stage("Build and Test") {
            steps {
                configFileProvider([configFile(fileId: '4f3d0128-0fdd-4de7-8536-5cbdd54a8baf', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh 'mvn -B -s "$MAVEN_SETTINGS_XML" clean install'
                }
            }

            post {
                always {
                    junit '**/failsafe-reports/*.xml,**/surefire-reports/*.xml'
                    jacoco()
                }
            }
        }

        stage("Sonarcloud") {
            when {
                not {
                    triggeredBy 'TimerTrigger'
                }
            }

            steps {
                configFileProvider([configFile(fileId: '4f3d0128-0fdd-4de7-8536-5cbdd54a8baf', variable: 'MAVEN_SETTINGS_XML')]) {
                    withSonarQubeEnv(installationName: 'Sonarcloud', credentialsId: 'e8795d01-550a-4c05-a4be-41b48b22403f') {
                        sh label: 'sonarcloud', script: "mvn -B -s \"$MAVEN_SETTINGS_XML\" -Dsonar.branch.name=${env.BRANCH_NAME} $SONAR_MAVEN_GOAL"
                    }
                }
            }
        }

        stage("Quality Gate") {
            when {
                not {
                    triggeredBy 'TimerTrigger'
                }
            }

            steps {
                timeout(time: 30, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage("Check Dependencies") {
            steps {
                dir('server') {
                    configFileProvider([configFile(fileId: '4f3d0128-0fdd-4de7-8536-5cbdd54a8baf', variable: 'MAVEN_SETTINGS_XML')]) {
                        sh 'mvn -B -s "$MAVEN_SETTINGS_XML" -Psecurity-scan dependency-check:check'
                    }
                    dependencyCheckPublisher failedTotalCritical: 1, failedTotalHigh: 4, failedTotalLow: 8, failedTotalMedium: 8, pattern: 'target/dependency-check-report.xml', unstableTotalCritical: 0, unstableTotalHigh: 2, unstableTotalLow: 8, unstableTotalMedium: 8
                }
            }
        }

        stage("Nexus Snapshot Deployment") {
            when {
                allOf {
                    not {
                        triggeredBy 'TimerTrigger'
                    }
                    branch "develop"
                }
            }

            steps {
                configFileProvider([configFile(fileId: '4f3d0128-0fdd-4de7-8536-5cbdd54a8baf', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh 'mvn -B -s "$MAVEN_SETTINGS_XML" -DskipTests -Dquarkus.package.type=uber-jar deploy'
                }
            }
        }

        stage("Nexus Release Deployment") {
            when {
                allOf {
                    not {
                        triggeredBy 'TimerTrigger'
                    }
                    branch "master"
                }
            }

            steps {
                configFileProvider([configFile(fileId: '4f3d0128-0fdd-4de7-8536-5cbdd54a8baf', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh 'mvn -B -s "$MAVEN_SETTINGS_XML" -DskipTests -Dquarkus.package.type=uber-jar deploy'
                }
            }
        }

        stage('Trigger Angular API package build') {
            when {
                not {
                    triggeredBy 'TimerTrigger'
                }
            }

            environment {
                VERSION = sh returnStdout: true, script: "mvn -B help:evaluate '-Dexpression=project.version' | grep -v '\\[' | tr -d '\\n'"
            }

            steps {
                build wait: false, job: '../astro-server-angular/master', parameters: [string(name: 'VERSION', value: env.VERSION)]
            }
        }

        stage('Build & Push Development Docker Image') {
            when {
                branch 'develop'
                not {
                    triggeredBy "TimerTrigger"
                }
            }

            parallel {
                stage("ARM64") {
                    agent {
                        label "arm64&&docker&&kotlin"
                    }

                    steps {
                        configFileProvider([configFile(fileId: '4f3d0128-0fdd-4de7-8536-5cbdd54a8baf', variable: 'MAVEN_SETTINGS_XML')]) {
                            sh "mvn -B -s \"$MAVEN_SETTINGS_XML\" clean install -DskipTests"
                        }
                        buildDockerImage("server", "latest-arm64")
                    }
                }

                stage("AMD64") {
                    steps {
                        buildDockerImage("server", "latest-amd64")
                    }
                }
            }
        }

        stage('Build Development Multi Arch Docker Manifest') {
            when {
                branch 'develop'
                not {
                    triggeredBy "TimerTrigger"
                }
            }

            steps {
                buildMultiArchManifest("latest")
            }
        }

        stage('Build & Push Release Docker Image') {
            when {
                branch 'master'
                not {
                    triggeredBy "TimerTrigger"
                }
            }

            parallel {
                stage("ARM64") {
                    agent {
                        label "arm64&&docker&&kotlin"
                    }

                    environment {
                        VERSION = sh returnStdout: true, script: "mvn -B help:evaluate '-Dexpression=project.version' | grep -v '\\[' | tr -d '\\n'"
                    }

                    steps {
                        configFileProvider([configFile(fileId: '4f3d0128-0fdd-4de7-8536-5cbdd54a8baf', variable: 'MAVEN_SETTINGS_XML')]) {
                            sh "mvn -B -s \"$MAVEN_SETTINGS_XML\" clean install -DskipTests"
                        }
                        buildDockerImage("server", env.VERSION + "-arm64")
                    }
                }

                stage("AMD64") {
                    environment {
                        VERSION = sh returnStdout: true, script: "mvn -B help:evaluate '-Dexpression=project.version' | grep -v '\\[' | tr -d '\\n'"
                    }

                    steps {
                        buildDockerImage("server", env.VERSION + "-amd64")
                    }
                }
            }
        }

        stage('Build Production Multi Arch Docker Manifest') {
            when {
                branch 'master'
                not {
                    triggeredBy "TimerTrigger"
                }
            }

            environment {
                VERSION = sh returnStdout: true, script: "mvn -B help:evaluate '-Dexpression=project.version' | grep -v '\\[' | tr -d '\\n'"
            }

            steps {
                buildMultiArchManifest(env.VERSION)
            }
        }

        stage('Trigger k8s deployment') {
            environment {
                VERSION = sh returnStdout: true, script: "mvn -B help:evaluate '-Dexpression=project.version' | grep -v '\\[' | tr -d '\\n'"
            }

            when {
                branch 'master'
                not {
                    triggeredBy "TimerTrigger"
                }
            }

            steps {
                build wait: false, job: '../astro-server-helm/master', parameters: [string(name: 'VERSION', value: env.VERSION)]
            }
        }
    }

    post {
        unsuccessful {
            mail to: "rafi@guengel.ch",
                    subject: "${JOB_NAME} (${BRANCH_NAME};${env.BUILD_DISPLAY_NAME}) -- ${currentBuild.currentResult}",
                    body: "Refer to ${currentBuild.absoluteUrl}"
        }
    }
}

def buildDockerImage(String fromDirectory, String tag) {
    withEnv(['IMAGE_TAG=' + tag]) {
        withCredentials([usernamePassword(credentialsId: '750504ce-6f4f-4252-9b2b-5814bd561430', passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]) {
            sh 'docker login --username "$USERNAME" --password "$PASSWORD"'
            configFileProvider([configFile(fileId: '4f3d0128-0fdd-4de7-8536-5cbdd54a8baf', variable: 'MAVEN_SETTINGS_XML')]) {
                dir(fromDirectory) {
                    sh '''mvn -B \
                        -s "${MAVEN_SETTINGS_XML}" \\
                        clean \\
                        package \\
                        -DskipTests \\
                        -Dquarkus.container-image.build=true \\
                        -Dquarkus.container-image.tag="${IMAGE_TAG}" \\
                        -Dquarkus.container-image.group=rafaelostertag \\
                        -Dquarkus.container-image.push=true \\
                        -Dquarkus.container-image.username="${USERNAME}" \\
                        -Dquarkus.container-image.password="${PASSWORD}"
                        '''
                }
            }
        }
    }
}

def buildMultiArchManifest(String tag) {
    withEnv(['IMAGE_TAG=' + tag]) {
        withCredentials([usernamePassword(credentialsId: '750504ce-6f4f-4252-9b2b-5814bd561430', passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]) {
            sh 'docker login --username "$USERNAME" --password "$PASSWORD"'
            sh 'docker manifest create "rafaelostertag/astro-server:${IMAGE_TAG}" --amend "rafaelostertag/astro-server:${IMAGE_TAG}-amd64" --amend "rafaelostertag/astro-server:${IMAGE_TAG}-arm64"'
            sh 'docker manifest push --purge "rafaelostertag/astro-server:${IMAGE_TAG}"'
        }
    }
}

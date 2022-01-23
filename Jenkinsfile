pipeline {
    agent {
        label 'amd64&&freebsd&&kotlin'
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
                    sh 'mvn -B -s "$MAVEN_SETTINGS_XML" install'
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
            steps {
                configFileProvider([configFile(fileId: '4f3d0128-0fdd-4de7-8536-5cbdd54a8baf', variable: 'MAVEN_SETTINGS_XML')]) {
                    withSonarQubeEnv(installationName: 'Sonarcloud', credentialsId: 'e8795d01-550a-4c05-a4be-41b48b22403f') {
                        sh label: 'sonarcloud', script: "mvn -B -s \"$MAVEN_SETTINGS_XML\" -Dsonar.branch.name=${env.BRANCH_NAME} $SONAR_MAVEN_GOAL"
                    }
                }
            }
        }

        stage("Quality Gate") {
            steps {
                timeout(time: 30, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage("Check Dependencies") {
            steps {
                configFileProvider([configFile(fileId: '4f3d0128-0fdd-4de7-8536-5cbdd54a8baf', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh 'mvn -B -s "$MAVEN_SETTINGS_XML" -Psecurity-scan dependency-check:check'
                }
                dependencyCheckPublisher failedTotalCritical: 1, failedTotalHigh: 4, failedTotalLow: 8, failedTotalMedium: 8, pattern: 'target/dependency-check-report.xml', unstableTotalCritical: 0, unstableTotalHigh: 2, unstableTotalLow: 8, unstableTotalMedium: 8
            }
        }

        stage("Nexus Deployment") {
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
                            sh "mvn -B -s \"$MAVEN_SETTINGS_XML\" clean package -DskipTests -Dquarkus.package.type=fast-jar"
                        }
                        sh "docker build -t rafaelostertag/astro-server:latest-arm64 -f src/main/docker/Dockerfile.jvm ."
                        withCredentials([usernamePassword(credentialsId: '750504ce-6f4f-4252-9b2b-5814bd561430', passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]) {
                            sh 'docker login --username "$USERNAME" --password "$PASSWORD"'
                            sh "docker push rafaelostertag/astro-server:latest-arm64"
                        }
                    }
                }

                stage("AMD64") {
                    agent {
                        label "amd64&&docker&&kotlin"
                    }

                    steps {
                        configFileProvider([configFile(fileId: '4f3d0128-0fdd-4de7-8536-5cbdd54a8baf', variable: 'MAVEN_SETTINGS_XML')]) {
                            sh "mvn -B -s \"$MAVEN_SETTINGS_XML\" clean package -DskipTests -Dquarkus.package.type=fast-jar"
                        }
                        sh "docker build -t rafaelostertag/astro-server:latest-amd64 -f src/main/docker/Dockerfile.jvm ."
                        withCredentials([usernamePassword(credentialsId: '750504ce-6f4f-4252-9b2b-5814bd561430', passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]) {
                            sh 'docker login --username "$USERNAME" --password "$PASSWORD"'
                            sh "docker push rafaelostertag/astro-server:latest-amd64"
                        }
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

            agent {
                label "docker"
            }

            steps {
                withCredentials([usernamePassword(credentialsId: '750504ce-6f4f-4252-9b2b-5814bd561430', passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]) {
                    sh 'docker login --username "$USERNAME" --password "$PASSWORD"'
                    sh 'docker manifest create "rafaelostertag/astro-server:latest" --amend "rafaelostertag/astro-server:latest-amd64" --amend "rafaelostertag/astro-server:latest-arm64"'
                    sh "docker manifest push --purge rafaelostertag/astro-server:latest"
                }
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
                            sh "mvn -B -s \"$MAVEN_SETTINGS_XML\" clean package -DskipTests -Dquarkus.package.type=fast-jar"
                        }
                        sh 'docker build -t rafaelostertag/astro-server:${VERSION}-arm64 -f src/main/docker/Dockerfile.jvm .'
                        withCredentials([usernamePassword(credentialsId: '750504ce-6f4f-4252-9b2b-5814bd561430', passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]) {
                            sh 'docker login --username "$USERNAME" --password "$PASSWORD"'
                            sh 'docker push rafaelostertag/astro-server:${VERSION}-arm64'
                        }
                    }
                }

                stage("AMD64") {
                    agent {
                        label "amd64&&docker&&kotlin"
                    }

                    environment {
                        VERSION = sh returnStdout: true, script: "mvn -B help:evaluate '-Dexpression=project.version' | grep -v '\\[' | tr -d '\\n'"
                    }

                    steps {
                        configFileProvider([configFile(fileId: '4f3d0128-0fdd-4de7-8536-5cbdd54a8baf', variable: 'MAVEN_SETTINGS_XML')]) {
                            sh "mvn -B -s \"$MAVEN_SETTINGS_XML\" clean package -DskipTests -Dquarkus.package.type=fast-jar"
                        }
                        sh 'docker build -t rafaelostertag/astro-server:${VERSION}-amd64 -f src/main/docker/Dockerfile.jvm .'
                        withCredentials([usernamePassword(credentialsId: '750504ce-6f4f-4252-9b2b-5814bd561430', passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]) {
                            sh 'docker login --username "$USERNAME" --password "$PASSWORD"'
                            sh 'docker push rafaelostertag/astro-server:${VERSION}-amd64'
                        }
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

            agent {
                label "docker&&kotlin"
            }

            environment {
                VERSION = sh returnStdout: true, script: "mvn -B help:evaluate '-Dexpression=project.version' | grep -v '\\[' | tr -d '\\n'"
            }

            steps {
                withCredentials([usernamePassword(credentialsId: '750504ce-6f4f-4252-9b2b-5814bd561430', passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]) {
                    sh 'docker login --username "$USERNAME" --password "$PASSWORD"'
                    sh 'docker manifest create "rafaelostertag/astro-server:${VERSION}" --amend "rafaelostertag/astro-server:${VERSION}-amd64" --amend "rafaelostertag/astro-server:${VERSION}-arm64"'
                    sh 'docker manifest push --purge rafaelostertag/astro-server:${VERSION}'
                }
            }
        }

//        stage('Trigger k8s deployment') {
//            environment {
//                VERSION = sh returnStdout: true, script: "mvn -B help:evaluate '-Dexpression=project.version' | grep -v '\\[' | tr -d '\\n'"
//            }
//
//            when {
//                branch 'master'
//                not {
//                    triggeredBy "TimerTrigger"
//                }
//            }
//
//            steps {
//                build wait: false, job: '../Helm/astro-server', parameters: [string(name: 'VERSION', value: env.VERSION)]
//            }
//        }
    }

    post {
        unsuccessful {
            mail to: "rafi@guengel.ch",
                    subject: "${JOB_NAME} (${BRANCH_NAME};${env.BUILD_DISPLAY_NAME}) -- ${currentBuild.currentResult}",
                    body: "Refer to ${currentBuild.absoluteUrl}"
        }
    }
}

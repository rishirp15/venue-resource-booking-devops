
pipeline {
    agent { label 'built-in' }

    tools {
        jdk 'JDK-21'
        maven 'Maven-3.9.16'
    }

    options {
        timestamps()
        skipDefaultCheckout(true)
    }

    environment {
        IMAGE_NAME = 'venue-booking'
        IMAGE_TAG = '1.0.0-rc.1'
    }

    stages {

        stage('Checkout') {
            steps {
                echo 'Checking out source code from GitHub...'
                checkout scm
            }
        }

        stage('Docker Check') {
            steps {
                echo 'Checking Docker installation and engine...'

                withEnv([
                    'PATH+DOCKER=C:\\Users\\RISHI\\AppData\\Local\\Programs\\DockerDesktop\\resources\\bin'
                ]) {
                    bat 'where docker'
                    bat 'docker version'
                    bat 'docker info'
                }
            }
        }

        stage('Build and Test') {
            steps {
                echo 'Building and testing Spring Boot application...'
                bat 'mvn clean verify'
            }

            post {
                always {
                    junit allowEmptyResults: true,
                          testResults: 'target/surefire-reports/*.xml,target/failsafe-reports/*.xml'
                }
            }
        }

        stage('Archive WAR') {
            steps {
                echo 'Archiving generated WAR file...'

                archiveArtifacts(
                    artifacts: 'target/*.war',
                    fingerprint: true
                )
            }
        }

        stage('Docker Build') {
            steps {
                echo 'Building Docker image...'

                withEnv([
                    'PATH+DOCKER=C:\\Users\\RISHI\\AppData\\Local\\Programs\\DockerDesktop\\resources\\bin'
                ]) {
                    bat 'docker build -t %IMAGE_NAME%:%IMAGE_TAG% .'
                }
            }
        }

        stage('Verify Docker Image') {
            steps {
                echo 'Verifying Docker image...'

                withEnv([
                    'PATH+DOCKER=C:\\Users\\RISHI\\AppData\\Local\\Programs\\DockerDesktop\\resources\\bin'
                ]) {
                    bat 'docker image inspect %IMAGE_NAME%:%IMAGE_TAG%'
                    bat 'docker images %IMAGE_NAME%'
                }
            }
        }
    }

    post {
        success {
            echo 'SUCCESS: Maven build, tests, WAR packaging and Docker image build completed.'
        }

        failure {
            echo 'FAILURE: Check the Jenkins console output for the failed stage.'
        }

        always {
            echo 'Jenkins pipeline execution finished.'
        }
    }
}

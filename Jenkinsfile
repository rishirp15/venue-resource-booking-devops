
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
        DOCKERHUB_USER = 'rishiphadale'
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
                withEnv([
                    'PATH+DOCKER=C:\\Users\\RISHI\\AppData\\Local\\Programs\\DockerDesktop\\resources\\bin'
                ]) {
                    bat 'where docker'
                    bat 'docker version'
                }
            }
        }

        stage('Build and Test') {
            steps {
                echo 'Building and testing application...'
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
                archiveArtifacts(
                    artifacts: 'target/*.war',
                    fingerprint: true
                )
            }
        }

        stage('Docker Build') {
            steps {
                withEnv([
                    'PATH+DOCKER=C:\\Users\\RISHI\\AppData\\Local\\Programs\\DockerDesktop\\resources\\bin'
                ]) {
                    bat 'docker build -t %IMAGE_NAME%:%IMAGE_TAG% .'
                }
            }
        }

        stage('Verify Docker Image') {
            steps {
                withEnv([
                    'PATH+DOCKER=C:\\Users\\RISHI\\AppData\\Local\\Programs\\DockerDesktop\\resources\\bin'
                ]) {
                    bat 'docker image inspect %IMAGE_NAME%:%IMAGE_TAG%'
                }
            }
        }

        stage('Push Docker Image') {
            steps {
                withEnv([
                    'PATH+DOCKER=C:\\Users\\RISHI\\AppData\\Local\\Programs\\DockerDesktop\\resources\\bin'
                ]) {
                    withCredentials([
                        usernamePassword(
                            credentialsId: 'dockerhub-credentials',
                            usernameVariable: 'DOCKER_USER',
                            passwordVariable: 'DOCKER_TOKEN'
                        )
                    ]) {
                        bat '''
                            @echo off
                            docker tag %IMAGE_NAME%:%IMAGE_TAG% %DOCKERHUB_USER%/%IMAGE_NAME%:%IMAGE_TAG%
                            if errorlevel 1 exit /b 1

                            echo Logging into Docker Hub...
                            echo %DOCKER_TOKEN% | docker login -u %DOCKER_USER% --password-stdin
                            if errorlevel 1 exit /b 1

                            echo Pushing Docker image...
                            docker push %DOCKERHUB_USER%/%IMAGE_NAME%:%IMAGE_TAG%
                            if errorlevel 1 exit /b 1

                            docker logout
                        '''
                    }
                }
            }
        }
    }

    post {
        success {
            echo 'SUCCESS: Application built, tested, packaged and pushed to Docker Hub.'
        }

        failure {
            echo 'FAILURE: Review Jenkins Console Output.'
        }

        always {
            echo 'Pipeline execution completed.'
        }
    }
}


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
                echo 'Checking Docker installation and engine...'

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
                echo 'Building and testing Spring Boot application...'

                bat 'mvn clean verify'
            }

            post {
                always {
                    junit(
                        allowEmptyResults: true,
                        testResults: 'target/surefire-reports/*.xml,target/failsafe-reports/*.xml'
                    )
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

        stage('Push Docker Image') {
            steps {
                echo 'Publishing Docker image to Docker Hub...'

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
                        powershell '''
                            $ErrorActionPreference = "Stop"

                            $image = "$env:DOCKERHUB_USER/$env:IMAGE_NAME`:$env:IMAGE_TAG"
                            $localImage = "$env:IMAGE_NAME`:$env:IMAGE_TAG"

                            Write-Host "Tagging Docker image..."

                            docker tag $localImage $image

                            if ($LASTEXITCODE -ne 0) {
                                throw "Docker image tagging failed"
                            }

                            Write-Host "Logging into Docker Hub..."

                            $token = $env:DOCKER_TOKEN.Trim()
                            $token | docker login --username $env:DOCKER_USER --password-stdin

                            if ($LASTEXITCODE -ne 0) {
                                throw "Docker Hub authentication failed"
                            }

                            try {
                                Write-Host "Pushing Docker image..."

                                docker push $image

                                if ($LASTEXITCODE -ne 0) {
                                    throw "Docker image push failed"
                                }

                                Write-Host "Docker image successfully published!"
                            }
                            finally {
                                docker logout
                            }
                        '''
                    }
                }
            }
        }
    }

    post {
        success {
            echo '=========================================='
            echo 'SUCCESS: CI/CD PIPELINE COMPLETED'
            echo 'Maven Build: SUCCESS'
            echo 'Automated Tests: SUCCESS'
            echo 'WAR Packaging: SUCCESS'
            echo 'Docker Build: SUCCESS'
            echo 'Docker Hub Push: SUCCESS'
            echo '=========================================='
        }

        failure {
            echo 'FAILURE: CI/CD pipeline failed.'
            echo 'Check Jenkins Console Output for details.'
        }

        always {
            echo 'Jenkins pipeline execution finished.'
        }
    }
}

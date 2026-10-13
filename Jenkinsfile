
pipeline {
    agent { label 'built-in' }

    tools {
        jdk 'JDK-21'
        maven 'Maven-3.9.16'
    }

    options {
        timestamps()
    }

    environment {
        IMAGE_NAME = 'venue-booking'
        IMAGE_TAG = '1.0.0-rc.1'
        PATH+DOCKER = 'C:\\Users\\RISHI\\AppData\\Local\\Programs\\DockerDesktop\\resources\\bin'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Docker Check') {
            steps {
                bat 'docker version'
                bat 'docker info'
            }
        }

        stage('Build and Test') {
            steps {
                bat 'mvn clean verify'
            }
        }

        stage('Archive WAR') {
            steps {
                archiveArtifacts artifacts: 'target/*.war',
                                 fingerprint: true
            }
        }

        stage('Docker Build') {
            steps {
                bat 'docker build -t %IMAGE_NAME%:%IMAGE_TAG% .'
            }
        }

        stage('Verify Docker Image') {
            steps {
                bat 'docker image inspect %IMAGE_NAME%:%IMAGE_TAG%'
            }
        }
    }

    post {
        success {
            echo 'Jenkins CI and Docker image build successful.'
        }

        failure {
            echo 'Pipeline failed. Review the failed stage.'
        }
    }
}

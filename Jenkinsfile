
pipeline {
    agent { label 'windows-docker' }

    tools {
        jdk 'JDK-21'
        maven 'Maven-3.9.16'
    }

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    environment {
        APP_NAME = 'venue-booking'
        APP_VERSION = '1.0.0-rc.1'
        DOCKER_IMAGE = 'venue-booking:1.0.0-rc.1'
        DOCKER_REPOSITORY = 'rishiphadale/venue-booking'
    }

    stages {

        stage('Checkout') {
            steps {
                echo 'Checking out source code...'
                checkout scm
            }
        }

        stage('Docker Check') {
            steps {
                echo 'Verifying Docker installation...'

                powershell '''
                    $ErrorActionPreference = "Stop"

                    docker --version

                    if ($LASTEXITCODE -ne 0) {
                        throw "Docker CLI is unavailable"
                    }

                    docker info --format '{{.ServerVersion}}'

                    if ($LASTEXITCODE -ne 0) {
                        throw "Docker Engine is unavailable"
                    }
                '''
            }
        }

        stage('Build and Test') {
            steps {
                echo 'Building and testing Spring Boot application...'

                bat 'mvn -B clean verify'
            }

            post {
                always {
                    junit allowEmptyResults: true,
                          testResults: 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Archive WAR') {
            steps {
                echo 'Archiving application WAR file...'

                archiveArtifacts artifacts: 'target/*.war',
                                 fingerprint: true,
                                 onlyIfSuccessful: true
            }
        }

        stage('Docker Build') {
            steps {
                echo 'Building Docker image...'

                powershell '''
                    $ErrorActionPreference = "Stop"

                    docker build -t $env:DOCKER_IMAGE .

                    if ($LASTEXITCODE -ne 0) {
                        throw "Docker image build failed"
                    }

                    Write-Host "Docker image built successfully!"
                '''
            }
        }

        stage('Verify Docker Image') {
            steps {
                echo 'Verifying Docker image...'

                powershell '''
                    $ErrorActionPreference = "Stop"

                    docker image inspect $env:DOCKER_IMAGE | Out-Null

                    if ($LASTEXITCODE -ne 0) {
                        throw "Docker image verification failed"
                    }

                    Write-Host "Docker image verified successfully!"
                '''
            }
        }

        stage('Push Docker Image') {
            steps {
                echo 'Publishing Docker image to Docker Hub...'

                withCredentials([
                    usernamePassword(
                        credentialsId: 'dockerhub-credentials',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_TOKEN'
                    )
                ]) {
                    powershell '''
                        $ErrorActionPreference = "Stop"

                        $remoteImage = "$env:DOCKER_REPOSITORY`:$env:APP_VERSION"

                        Write-Host "Tagging Docker image..."
                        Write-Host "Target: $remoteImage"

                        docker tag $env:DOCKER_IMAGE $remoteImage

                        if ($LASTEXITCODE -ne 0) {
                            throw "Docker image tagging failed"
                        }

                        # Create temporary authentication directory
                        $tempDir = Join-Path $env:TEMP (
                            "venue-docker-" +
                            [guid]::NewGuid().ToString("N")
                        )

                        New-Item -ItemType Directory `
                            -Path $tempDir -Force | Out-Null

                        $tokenFile = Join-Path $tempDir "token.txt"
                        $dockerConfig = Join-Path $tempDir "config"

                        New-Item -ItemType Directory `
                            -Path $dockerConfig -Force | Out-Null

                        $oldDockerConfig = $env:DOCKER_CONFIG

                        try {
                            # Write PAT using UTF-8 without BOM
                            $utf8 = New-Object System.Text.UTF8Encoding($false)

                            [System.IO.File]::WriteAllText(
                                $tokenFile,
                                $env:DOCKER_TOKEN + "`n",
                                $utf8
                            )

                            # Use isolated Docker credentials
                            $env:DOCKER_CONFIG = $dockerConfig

                            Write-Host "Logging into Docker Hub..."

                            # CMD redirects raw UTF-8 file bytes to Docker
                            $command = 'docker login --username ' +
                                $env:DOCKER_USER +
                                ' --password-stdin < "' +
                                $tokenFile + '"'

                            & cmd.exe /d /c $command

                            if ($LASTEXITCODE -ne 0) {
                                throw "Docker Hub authentication failed"
                            }

                            Write-Host "Docker Hub login successful!"

                            # Delete plaintext PAT immediately
                            Remove-Item -Path $tokenFile -Force

                            Write-Host "Pushing Docker image..."

                            docker push $remoteImage

                            if ($LASTEXITCODE -ne 0) {
                                throw "Docker image push failed"
                            }

                            Write-Host "Docker image pushed successfully!"
                            Write-Host "Image: $remoteImage"
                        }
                        finally {
                            # Restore Docker configuration
                            $env:DOCKER_CONFIG = $oldDockerConfig

                            # Delete temporary Docker credentials
                            Remove-Item -Path $tempDir `
                                -Recurse -Force `
                                -ErrorAction SilentlyContinue
                        }
                    '''
                }
            }
        }

        stage('Deploy with Ansible') {
        steps {
            echo 'Deploying Venue Booking to Ubuntu using Ansible...'

            powershell '''
            $ErrorActionPreference = "Stop"

            $sshKey = "R:\\JenkinsAgent\\ssh\\venue_deploy"
            $target = "hduser@192.168.56.101"

            & ssh.exe `
                -i $sshKey `
                -o BatchMode=yes `
                -o StrictHostKeyChecking=yes `
                $target `
                "cd ~/venue-booking-ansible && ansible-playbook -i inventory.ini deploy.yml"

            if ($LASTEXITCODE -ne 0) {
                throw "Ansible deployment failed"
            }

            Write-Host "Ansible deployment completed successfully!"
        '''
        }
}
    }

    post {
        success {
            echo '========================================'
            echo ' CI/CD PIPELINE COMPLETED SUCCESSFULLY'
            echo '========================================'
            echo "Docker image: ${env.DOCKER_REPOSITORY}:${env.APP_VERSION}"
        }

        failure {
            echo 'CI/CD pipeline failed. Check the failed stage logs.'
        }

        always {
            echo "Build completed: ${env.JOB_NAME} #${env.BUILD_NUMBER}"
        }
    }
}

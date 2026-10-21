
pipeline {
    agent any

    tools {
        jdk 'JDK21'
        maven 'Maven'
    }

    environment {
        IMAGE_NAME = 'tenant-complaint-management-system'
        IMAGE_TAG = "${BUILD_NUMBER}"
        LATEST_TAG = 'latest'
        DOCKERHUB_CREDENTIALS_ID = 'dockerhub-credentials'
        APP_URL = 'http://localhost:8080'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Clean') {
            steps {
                bat 'mvn clean'
            }
        }

        stage('Unit Test') {
            steps {
                bat 'mvn test'
            }
            post {
                always {
                    junit allowEmptyResults: true,
                          testResults: 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Package') {
            steps {
                bat 'mvn package -DskipTests'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.war',
                                     fingerprint: true
                }
            }
        }

        stage('Selenium Tests') {
            environment {
                JENKINS_NODE_COOKIE = 'dontKillMe'
            }

            steps {
                script {
                    // Ensure the log directory exists.
                    bat '''
                        @echo off
                        if not exist target mkdir target
                        if exist target\\app-server.log del /q target\\app-server.log
                    '''

                    // Start the application in the background.
                    bat '''
                        @echo off
                        start "" /B cmd /c "mvn spring-boot:run -Dspring-boot.run.profiles=test -Dspring-boot.run.useTestClasspath=true > target\\app-server.log 2>&1"
                    '''

                    // Wait indefinitely for the application to become ready.
                    // There is no fixed 120-second timeout.
                    bat '''
                        @echo off
                        powershell -NoProfile -ExecutionPolicy Bypass -Command ^
                        "$ErrorActionPreference = 'Continue'; ^
                        Write-Host 'Waiting for application startup...'; ^
                        while ($true) { ^
                            try { ^
                                $response = Invoke-WebRequest -Uri 'http://localhost:8080/login' -UseBasicParsing -TimeoutSec 5 -MaximumRedirection 5; ^
                                if ([int]$response.StatusCode -ge 200 -and [int]$response.StatusCode -lt 400) { ^
                                    Write-Host 'Application is ready.'; ^
                                    exit 0; ^
                                } ^
                            } catch { ^
                                $status = 0; ^
                                if ($_.Exception.Response) { ^
                                    $status = [int]$_.Exception.Response.StatusCode; ^
                                }; ^
                                if ($status -ge 200 -and $status -lt 400) { ^
                                    Write-Host 'Application is ready.'; ^
                                    exit 0; ^
                                }; ^
                            }; ^
                            if (Test-Path 'target\\app-server.log') { ^
                                $log = Get-Content 'target\\app-server.log' -Raw -ErrorAction SilentlyContinue; ^
                                if ($log -match 'APPLICATION FAILED TO START|BUILD FAILURE|Unable to start web server|Port 8080 was already in use') { ^
                                    Write-Host 'Application startup failure detected. Server log follows:'; ^
                                    Get-Content 'target\\app-server.log'; ^
                                    exit 1; ^
                                }; ^
                            }; ^
                            Write-Host ('Application is not ready yet. Retrying at ' + (Get-Date -Format 'HH:mm:ss')); ^
                            Start-Sleep -Seconds 10; ^
                        }"
                        if errorlevel 1 (
                            echo Application startup failed.
                            if exist target\\app-server.log type target\\app-server.log
                            exit /b 1
                        )
                    '''

                    // Execute Selenium integration tests only after readiness.
                    bat 'mvn failsafe:integration-test failsafe:verify -Dapp.base.url=%APP_URL%'
                }
            }

            post {
                always {
                    // Display application logs if the stage failed.
                    script {
                        if (currentBuild.currentResult == 'FAILURE') {
                            bat '''
                                @echo off
                                echo ===== APPLICATION SERVER LOG =====
                                if exist target\\app-server.log (
                                    type target\\app-server.log
                                ) else (
                                    echo Application log was not created.
                                )
                            '''
                        }
                    }

                    // Stop the application listening on port 8080.
                    bat '''
                        @echo off
                        powershell -NoProfile -ExecutionPolicy Bypass -Command ^
                        "Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }; exit 0"
                    '''

                    junit allowEmptyResults: true,
                          testResults: 'target/failsafe-reports/*.xml'

                    archiveArtifacts artifacts: 'target/selenium-screenshots/*.png',
                                     allowEmptyArchive: true

                    archiveArtifacts artifacts: 'target/app-server.log',
                                     allowEmptyArchive: true
                }
            }
        }

        stage('Docker Build') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: DOCKERHUB_CREDENTIALS_ID,
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASSWORD'
                    )
                ]) {
                    bat '''
                        docker build -t "%DOCKER_USER%/%IMAGE_NAME%:%IMAGE_TAG%" .
                        if errorlevel 1 exit /b 1

                        docker tag "%DOCKER_USER%/%IMAGE_NAME%:%IMAGE_TAG%" "%DOCKER_USER%/%IMAGE_NAME%:%LATEST_TAG%"
                        if errorlevel 1 exit /b 1
                    '''
                }
            }
        }

        stage('Docker Push') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: DOCKERHUB_CREDENTIALS_ID,
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASSWORD'
                    )
                ]) {
                    bat '''
                        @echo off
                        powershell -NoProfile -Command "$env:DOCKER_PASSWORD | docker login --username $env:DOCKER_USER --password-stdin"
                        if errorlevel 1 exit /b 1

                        docker push "%DOCKER_USER%/%IMAGE_NAME%:%IMAGE_TAG%"
                        if errorlevel 1 exit /b 1

                        docker push "%DOCKER_USER%/%IMAGE_NAME%:%LATEST_TAG%"
                        if errorlevel 1 exit /b 1
                    '''
                }
            }
        }

        stage('Ansible Deployment') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: DOCKERHUB_CREDENTIALS_ID,
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASSWORD'
                    )
                ]) {
                    bat '''
                        @echo off
                        wsl bash -lc "cd /mnt/c/Users/Mahesh/Desktop/tenant_complaint_management_system && ansible-playbook -i ansible/inventory.ini ansible/site.yml -e docker_image=%DOCKER_USER%/%IMAGE_NAME%:%IMAGE_TAG%"
                        if errorlevel 1 exit /b 1
                    '''
                }
            }
        }
    }

    post {
        success {
            echo 'CI/CD pipeline completed successfully!'
        }

        failure {
            echo 'Pipeline failed. Check the logs of the failed stage.'
        }

        always {
            echo 'Jenkins pipeline execution finished.'
        }
    }
}

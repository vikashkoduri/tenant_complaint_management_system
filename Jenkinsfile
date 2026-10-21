
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
                // Run unit tests without starting SeleniumTest prematurely.
                bat 'mvn test "-Dtest=!SeleniumTest"'
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
                    bat '''
                        @echo off
                        if not exist target mkdir target
                        if exist target\\app-server.log del /q target\\app-server.log
                        if exist wait-for-app.ps1 del /q wait-for-app.ps1
                    '''

                    // Start Spring Boot in the background.
                    bat '''
                        @echo off
                        start "" /B cmd /c "mvn spring-boot:run -Dspring-boot.run.profiles=test -Dspring-boot.run.useTestClasspath=true > target\\app-server.log 2>&1"
                    '''

                    // Write a separate PowerShell script.
                    // No CMD caret characters are used inside PowerShell.
                    writeFile file: 'wait-for-app.ps1', text: '''
$ErrorActionPreference = 'Continue'
$uri = 'http://localhost:8080/login'
$logFile = 'target\\app-server.log'

Write-Host "Waiting for the application to start."
Write-Host "Checking: $uri"
Write-Host "There is no fixed 120-second timeout."

while ($true) {
    $ready = $false

    try {
        $response = Invoke-WebRequest -Uri $uri -UseBasicParsing -TimeoutSec 5
        $statusCode = [int]$response.StatusCode

        if ($statusCode -ge 200 -and $statusCode -lt 400) {
            $ready = $true
        }
    }
    catch {
        try {
            if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
                $statusCode = [int]$_.Exception.Response.StatusCode

                if ($statusCode -ge 200 -and $statusCode -lt 400) {
                    $ready = $true
                }
            }
        }
        catch {
            # The application is not reachable yet.
        }
    }

    if ($ready) {
        Write-Host "Application is ready. Starting Selenium tests."
        exit 0
    }

    if (Test-Path $logFile) {
        $log = Get-Content $logFile -Raw -ErrorAction SilentlyContinue

        if ($log -match 'APPLICATION FAILED TO START|Unable to start web server|Port 8080 was already in use') {
            Write-Host "Spring Boot startup error detected."
            Get-Content $logFile
            exit 1
        }
    }

    Write-Host "Application is not ready. Retrying in 10 seconds..."
    Start-Sleep -Seconds 10
}
'''

                    // Execute the script as a file, not as an inline command.
                    bat '''
                        @echo off
                        powershell.exe -NoProfile -ExecutionPolicy Bypass -File wait-for-app.ps1

                        if errorlevel 1 (
                            echo Application startup check failed.
                            if exist target\\app-server.log type target\\app-server.log
                            exit /b 1
                        )
                    '''

                    // Run Selenium after the application responds.
                    bat 'mvn failsafe:integration-test failsafe:verify -Dapp.base.url=%APP_URL%'
                }
            }

            post {
                always {
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
                        powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }; exit 0"
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
                        @echo off
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
                        powershell.exe -NoProfile -Command "$env:DOCKER_PASSWORD | docker login --username $env:DOCKER_USER --password-stdin"
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
            echo 'Pipeline failed. Check the failed stage logs.'
        }

        always {
            echo 'Jenkins pipeline execution finished.'
        }
    }
}

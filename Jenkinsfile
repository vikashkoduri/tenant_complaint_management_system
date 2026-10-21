// pipeline {
//     agent any

//     environment {
//         // App configuration
//         IMAGE_NAME = 'tenant-complaint-management-system'
//         DOCKERHUB_CREDENTIALS_ID = 'dockerhub-credentials'
//         DOCKERHUB_USERNAME = credentials("${DOCKERHUB_CREDENTIALS_ID}")
        
//         // Define tags based on branch/build
//         IMAGE_TAG = "${env.BUILD_NUMBER}"
//         LATEST_TAG = 'latest'
//     }

//     stages {
//         stage('Checkout') {
//             steps {
//                 checkout scm
//             }
//         }

//         stage('Clean') {
//             steps {
//                 sh './mvnw clean || mvn clean'
//             }
//         }

//         stage('Unit Test') {
//             steps {
//                 sh './mvnw test -Dtest="!*SeleniumTest" || mvn test -Dtest="!*SeleniumTest"'
//             }
//             post {
//                 always {
//                     junit 'target/surefire-reports/*.xml'
//                 }
//             }
//         }

//         stage('Package') {
//             steps {
//                 sh './mvnw package -DskipTests || mvn package -DskipTests'
//             }
//             post {
//                 success {
//                     archiveArtifacts artifacts: 'target/*.war', fingerprint: true
//                 }
//             }
//         }

//         stage('Integration & Selenium Tests') {
//             steps {
//                 // Ensure the app can start up and Selenium tests run
//                 sh './mvnw failsafe:integration-test failsafe:verify || mvn failsafe:integration-test failsafe:verify'
//             }
//             post {
//                 always {
//                     junit 'target/failsafe-reports/*.xml'
//                     archiveArtifacts artifacts: 'target/selenium-screenshots/*.png', allowEmptyArchive: true
//                 }
//             }
//         }

//         stage('Docker Build') {
//             steps {
//                 script {
//                     dockerImage = docker.build("${DOCKERHUB_USERNAME}/${IMAGE_NAME}:${IMAGE_TAG}")
//                 }
//             }
//         }

//         stage('Docker Tag & Push') {
//             steps {
//                 script {
//                     docker.withRegistry('', DOCKERHUB_CREDENTIALS_ID) {
//                         dockerImage.push()
//                         dockerImage.push("${LATEST_TAG}")
//                     }
//                 }
//             }
//         }

//         stage('Deploy to Environment') {
//             environment {
//                 ANSIBLE_INVENTORY = 'ansible/inventory.ini'
//                 // Assuming Ansible credentials exist in Jenkins
//                 SSH_CREDENTIALS_ID = 'target-server-ssh'
//             }
//             steps {
//                 // In a real scenario, you'd use the Ansible plugin or run it via bash.
//                 // We're using standard bash here for portability in the college project.
//                 script {
//                     withCredentials([sshUserPrivateKey(credentialsId: SSH_CREDENTIALS_ID, keyFileVariable: 'SSH_KEY', usernameVariable: 'SSH_USER')]) {
//                         sh """
//                         ansible-playbook -i ${ANSIBLE_INVENTORY} ansible/site.yml \
//                             -e "docker_image=${DOCKERHUB_USERNAME}/${IMAGE_NAME}:${IMAGE_TAG}" \
//                             -e "ansible_user=${SSH_USER}" \
//                             --private-key ${SSH_KEY}
//                         """
//                     }
//                 }
//             }
//         }
//     }

//     post {
//         always {
//             cleanWs()
//         }
//         success {
//             echo "Pipeline completed successfully!"
//         }
//         failure {
//             echo "Pipeline failed! Please check the logs."
//         }
//     }
// }

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
                    junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Package') {
            steps {
                bat 'mvn package -DskipTests'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.war', fingerprint: true
                }
            }
        }

        stage('Selenium Tests') {
            steps {
                // Start the Spring Boot application in the background
                bat 'start /B mvn spring-boot:run -Dspring-boot.run.profiles=test > target\\app-server.log 2>&1'

                // Wait for the application to be ready (up to 120 seconds)
                bat '''
                    @echo off
                    set RETRIES=0
                    :LOOP
                    if %RETRIES% GEQ 24 (
                        echo Application failed to start within 120 seconds
                        exit /b 1
                    )
                    curl -s -o nul -w "%%{http_code}" http://localhost:8080/actuator/health | findstr "200" >nul 2>&1
                    if %ERRORLEVEL% EQU 0 (
                        echo Application is ready
                        goto :RUN_TESTS
                    )
                    echo Waiting for application to start... attempt %RETRIES%
                    timeout /t 5 /nobreak >nul
                    set /a RETRIES=%RETRIES%+1
                    goto :LOOP
                    :RUN_TESTS
                '''

                // Run the Selenium tests
                bat 'mvn failsafe:integration-test failsafe:verify -Dapp.base.url=http://localhost:8080'
            }
            post {
                always {
                    // Stop the Spring Boot application
                    bat 'taskkill /F /FI "WINDOWTITLE eq mvn*" 2>nul & echo App cleanup done'
                    junit allowEmptyResults: true, testResults: 'target/failsafe-reports/*.xml'
                    archiveArtifacts artifacts: 'target/selenium-screenshots/*.png', allowEmptyArchive: true
                }
            }
                    bat 'for /f "tokens=5" %%a in (\'netstat -aon ^| findstr ":8080" ^| findstr "LISTENING"\') do taskkill /F /PID %%a 2>nul || echo "App already stopped"'
                    junit allowEmptyResults: true, testResults: 'target/failsafe-reports/*.xml'
                    archiveArtifacts artifacts: 'target/selenium-screenshots/*.png', allowEmptyArchive: true
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
                    bat 'docker build -t %DOCKER_USER%/%IMAGE_NAME%:%IMAGE_TAG% .'
                    bat 'docker tag %DOCKER_USER%/%IMAGE_NAME%:%IMAGE_TAG% %DOCKER_USER%/%IMAGE_NAME%:%LATEST_TAG%'
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
                    bat 'echo %DOCKER_PASSWORD% | docker login --username %DOCKER_USER% --password-stdin'
                    bat 'docker push %DOCKER_USER%/%IMAGE_NAME%:%IMAGE_TAG%'
                    bat 'docker push %DOCKER_USER%/%IMAGE_NAME%:%LATEST_TAG%'
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
                    bat 'wsl bash -lc "cd /mnt/c/Users/Mahesh/Desktop/tenant_complaint_management_system && ansible-playbook -i ansible/inventory.ini ansible/site.yml -e docker_image=%DOCKER_USER%/%IMAGE_NAME%:%IMAGE_TAG%"'
                }
            }
        }
    }

    post {
        success {
            echo 'CI/CD Pipeline completed successfully!'
        }

        failure {
            echo 'Pipeline failed. Check the failed stage above.'
        }
    }
}
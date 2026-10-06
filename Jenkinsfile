pipeline {
    agent any

    environment {
        // App configuration
        IMAGE_NAME = 'tenant-complaint-management-system'
        DOCKERHUB_CREDENTIALS_ID = 'dockerhub-credentials'
        DOCKERHUB_USERNAME = credentials("${DOCKERHUB_CREDENTIALS_ID}")
        
        // Define tags based on branch/build
        IMAGE_TAG = "${env.BUILD_NUMBER}"
        LATEST_TAG = 'latest'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Clean') {
            steps {
                sh './mvnw clean || mvn clean'
            }
        }

        stage('Unit Test') {
            steps {
                sh './mvnw test -Dtest="!*SeleniumTest" || mvn test -Dtest="!*SeleniumTest"'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Package') {
            steps {
                sh './mvnw package -DskipTests || mvn package -DskipTests'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.war', fingerprint: true
                }
            }
        }

        stage('Integration & Selenium Tests') {
            steps {
                // Ensure the app can start up and Selenium tests run
                sh './mvnw failsafe:integration-test failsafe:verify || mvn failsafe:integration-test failsafe:verify'
            }
            post {
                always {
                    junit 'target/failsafe-reports/*.xml'
                    archiveArtifacts artifacts: 'target/selenium-screenshots/*.png', allowEmptyArchive: true
                }
            }
        }

        stage('Docker Build') {
            steps {
                script {
                    dockerImage = docker.build("${DOCKERHUB_USERNAME}/${IMAGE_NAME}:${IMAGE_TAG}")
                }
            }
        }

        stage('Docker Tag & Push') {
            steps {
                script {
                    docker.withRegistry('', DOCKERHUB_CREDENTIALS_ID) {
                        dockerImage.push()
                        dockerImage.push("${LATEST_TAG}")
                    }
                }
            }
        }

        stage('Deploy to Environment') {
            environment {
                ANSIBLE_INVENTORY = 'ansible/inventory.ini'
                // Assuming Ansible credentials exist in Jenkins
                SSH_CREDENTIALS_ID = 'target-server-ssh'
            }
            steps {
                // In a real scenario, you'd use the Ansible plugin or run it via bash.
                // We're using standard bash here for portability in the college project.
                script {
                    withCredentials([sshUserPrivateKey(credentialsId: SSH_CREDENTIALS_ID, keyFileVariable: 'SSH_KEY', usernameVariable: 'SSH_USER')]) {
                        sh """
                        ansible-playbook -i ${ANSIBLE_INVENTORY} ansible/site.yml \
                            -e "docker_image=${DOCKERHUB_USERNAME}/${IMAGE_NAME}:${IMAGE_TAG}" \
                            -e "ansible_user=${SSH_USER}" \
                            --private-key ${SSH_KEY}
                        """
                    }
                }
            }
        }
    }

    post {
        always {
            cleanWs()
        }
        success {
            echo "Pipeline completed successfully!"
        }
        failure {
            echo "Pipeline failed! Please check the logs."
        }
    }
}

# Dockerized Tenant Complaint Management System

A complete DevOps college project for managing tenant complaints.

## 🌟 Features

*   **Tenant Portal:** Submit complaints, attach documents, and track status.
*   **Reviewer Dashboard:** Login securely, view complaints, update status, and manage the resolution lifecycle.
*   **DevOps Pipeline:** Fully automated CI/CD pipeline with Jenkins, Docker, and Ansible.

## 🏗️ Architecture

*   **Backend:** Java 21, Spring Boot 3.5.x
*   **Frontend:** HTML, CSS (Custom Design System), Thymeleaf
*   **Database:** MySQL 8.4 LTS
*   **Testing:** JUnit, Mockito, Selenium WebDriver
*   **Containerization:** Docker & Docker Compose
*   **CI/CD:** Jenkins
*   **Configuration Management:** Ansible

## 📁 Project Structure

```text
tenant-complaint-management-system/
│
├── src/                  # Source code (Java, Resources, Templates)
├── src/test/             # Unit, Integration, and Selenium tests
├── docker-compose.yml    # Local multi-container setup
├── Dockerfile            # Multi-stage production build
├── Jenkinsfile           # CI/CD Pipeline definition
├── pom.xml               # Maven configuration
├── ansible/              # Deployment playbooks and inventory
└── .env.example          # Example environment variables
```

## 🚀 Prerequisites

Ensure the following are installed:
*   Java 21
*   Maven 3.9+
*   MySQL 8.4
*   Docker & Docker Compose
*   Jenkins
*   Ansible

## ⚙️ Environment Setup

1.  Copy `.env.example` to `.env` and configure your database credentials.
2.  If running locally without Docker, ensure MySQL is running and a database named `tenant_complaints` exists.

## 💻 Local Development

### Running the Application

Using Maven (Spring Boot Plugin):
```bash
mvn spring-boot:run
```

The application will be available at `http://localhost:8080`.

### Running Tests

Run Unit and Integration Tests (excludes Selenium):
```bash
mvn clean test
```

Run Selenium WebDriver Tests:
*Note: Requires Chrome installed. The driver is managed automatically by Selenium.*
```bash
mvn clean verify
```

### Building the Project

Package as an executable WAR (can run standalone or in Tomcat):
```bash
mvn clean package -DskipTests
```

## 🐳 Docker Deployment

### Using Docker Compose (Recommended for local testing)

Start the application and MySQL database together:
```bash
docker compose up -d
```
Access the app at `http://localhost:8080`.

Stop the containers:
```bash
docker compose down
```

### Building the Image Manually

```bash
docker build -t tenant-complaint-management-system:1.0.0 .
```

## 🔄 CI/CD Pipeline (Jenkins)

The included `Jenkinsfile` provides a complete pipeline:
1.  **Checkout:** Pulls code from Git.
2.  **Clean & Test:** Runs Maven tests.
3.  **Package:** Builds the WAR file.
4.  **Selenium Tests:** Verifies core user journeys.
5.  **Docker Build & Push:** Creates the image and pushes to Docker Hub.
6.  **Deploy:** Executes Ansible playbook.

**Jenkins Setup:**
1. Create a Pipeline Job in Jenkins.
2. Point it to your GitHub repository.
3. Add a Global Credential (Username with password) named `dockerhub-credentials`.
4. (Optional) Add SSH credentials for Ansible target deployment named `target-server-ssh`.

## 🛠️ Ansible Deployment

The playbook in `ansible/site.yml` handles provisioning the target server.

Execute manually:
```bash
ansible-playbook -i ansible/inventory.ini ansible/site.yml
```

## 🏥 Health Check

The application exposes a Spring Boot Actuator endpoint for health monitoring:
```text
http://localhost:8080/actuator/health
```

## 🌐 Tomcat Deployment

To deploy to an external Apache Tomcat 10.1.x server:
1. Build the WAR: `mvn clean package -DskipTests`
2. Copy `target/tenant-complaint-management-system.war` to Tomcat's `webapps/` directory.
3. Start Tomcat.

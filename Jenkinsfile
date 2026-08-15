pipeline {
    agent {
        label 'home-server'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build Backend') {
            steps {
                dir('backend') {
                    sh './mvnw clean package -DskipTests'
                }
            }
        }

        stage('Test Backend') {
            steps {
                dir('backend') {
                    sh './mvnw test'
                }
            }
        }

        stage('Build Frontend') {
            steps {
                dir('frontend') {
                    sh 'npm install'
                    sh 'npm run build'
                }
            }
        }

        stage('Test Frontend') {
            // Requires a Chrome/Chromium install on the Jenkins agent for
            // ChromeHeadless -- set CHROME_BIN if it's not on PATH.
            steps {
                dir('frontend') {
                    sh 'npx ng test --no-watch --no-progress --browsers=ChromeHeadless'
                }
            }
        }

        stage('Deploy') {
            steps {
                sh 'docker compose down'
                sh 'docker compose up --build -d'
            }
        }

    }
}

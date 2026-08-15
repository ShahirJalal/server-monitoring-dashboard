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
            // Uses the Chromium that `puppeteer` bundles (installed by `npm install`
            // above) instead of relying on a system Chrome/Chromium on the agent --
            // avoids fighting Ubuntu's snap-packaged chromium in headless CI.
            steps {
                dir('frontend') {
                    sh 'CHROME_BIN=$(node -e "console.log(require(\'puppeteer\').executablePath())") npx ng test --no-watch --no-progress --browsers=ChromeHeadless'
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

pipeline {

    agent {
        label 'jenkins-agent-machine-1'
    }

    stages {

        stage('Build') {
            steps {
                sh './gradlew build --refresh-dependencies --no-daemon'
            }
        }

        stage('Test') {
            steps {
                withCredentials([string(credentialsId: 'ADDRESS_SONAR', variable: 'address_sonar'),
                                 string(credentialsId: 'SONAR_TOKEN',   variable: 'sonar_token')]) {
                    sh './gradlew clean test itest sonarqube -Dsonar.projectKey=iexec-sms -Dsonar.host.url=$address_sonar -Dsonar.login=$sonar_token --refresh-dependencies --no-daemon'
                }
                junit 'build/test-results/**/*.xml'
                jacoco()
            }
        }

        stage('Upload Jars') {
            when {
                anyOf{
                    branch 'master'
                    branch 'develop'
                }
            }
            steps {
                withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'nexus', usernameVariable: 'NEXUS_USER', passwordVariable: 'NEXUS_PASSWORD']]) {
                    sh './gradlew -PnexusUser=$NEXUS_USER -PnexusPassword=$NEXUS_PASSWORD publish --no-daemon'
                }
            }
        }

        stage('Build/Upload Docker image') {
            when {
                anyOf {
                    branch 'master'
                    branch 'develop'
                    branch 'feature/*'
                }
            }
            steps {
                withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'nexus', usernameVariable: 'NEXUS_USER', passwordVariable: 'NEXUS_PASSWORD']]) {
                    sh './gradlew -PnexusUser=$NEXUS_USER -PnexusPassword=$NEXUS_PASSWORD pushImage --no-daemon'
                }
            }
        }
    }

}

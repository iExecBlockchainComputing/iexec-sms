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

        stage('Upload Jars') {
            when {
                branch 'master'
            }
            steps {
                withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'nexus', usernameVariable: 'NEXUS_USER', passwordVariable: 'NEXUS_PASSWORD']]) {
                    sh './gradlew -PnexusUser=$NEXUS_USER -PnexusPassword=$NEXUS_PASSWORD uploadArchives --no-daemon'
                }
            }
        }

        stage('Build/Upload Docker image') {
            when {
                branch 'master'
            }
            steps {
                withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'nexus', usernameVariable: 'NEXUS_USER', passwordVariable: 'NEXUS_PASSWORD']]) {
                    sh './gradlew -PnexusUser=$NEXUS_USER -PnexusPassword=$NEXUS_PASSWORD pushImage --no-daemon'
                }
            }
        }
    }

}

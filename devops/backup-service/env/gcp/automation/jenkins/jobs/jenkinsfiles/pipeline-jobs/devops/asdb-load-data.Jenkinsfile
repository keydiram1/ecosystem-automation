@Library("jenkins-sqlite-lib") _
pipeline {
    agent {
        label "built-in"
    }
    triggers {
        cron("0 5 * * *")
    }
    stages {
        stage("Run ASDB Data Loader") {
            steps {
                script {
                    LoadAsdbData()
                }
            }
        }
    }
}

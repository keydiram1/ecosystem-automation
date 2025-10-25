@Library("jenkins-sqlite-lib") _
pipeline {
    agent {
        label "built-in"
    }
    parameters {
        string(name: "ws", defaultValue: "ws-1")
    }
    stages {
        stage("Remove Workspace") {
            steps {
                script {
                    RemoveWorkspace("${ws}")
                }
            }
        }
    }
}

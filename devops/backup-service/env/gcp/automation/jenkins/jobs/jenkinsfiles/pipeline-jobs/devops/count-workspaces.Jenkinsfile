@Library("jenkins-sqlite-lib") _
pipeline {
    agent {
        label "built-in"
    }
    parameters {
        string(name: "gke-name", defaultValue: "ecosys-gke")
    }
    stages {
        stage("Count Deployed Workspaces") {
            steps {
                script {
                    int count = CountWorkspaces()
                    currentBuild.description = "${count}"
                }
            }
        }
    }
}

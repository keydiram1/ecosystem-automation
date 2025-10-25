@Library("jenkins-sqlite-lib") _
pipeline {
    agent {
        label "built-in"
    }
    parameters {
        string(name: "gke-name", defaultValue: "ecosys-gke")
    }
    stages {
        stage("Search For Cloud Environment") {
            steps {
                script {
                    boolean isExists = EnvironmentExists(["gke_name": params['gke-name']])
                    currentBuild.description = "${isExists}"
                }
            }
        }
    }
}

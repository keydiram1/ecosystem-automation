@Library("jenkins-sqlite-lib") _
pipeline {
    agent {
        label "built-in"
    }
    parameters {
        string(name: "gke-name", defaultValue: "ecosys-gke")
    }
    stages {
        stage("Remove Cloud Environment") {
            steps {
                script {
                    RemoveEnvironment([
                        "gke_name": params['gke-name']
                    ])
                }
            }
        }
    }
}

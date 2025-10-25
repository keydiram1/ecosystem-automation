@Library("jenkins-sqlite-lib") _
pipeline {
    agent {
        label "built-in"
    }
    parameters {
        string(name: "gke-name", defaultValue: "ecosys-gke")
    }
    stages {
        stage("Add Cloud Environment") {
            steps {
                script {
                    AddEnvironment([
                        "gke_name": params['gke-name']
                    ])
                }
            }
        }
    }
}

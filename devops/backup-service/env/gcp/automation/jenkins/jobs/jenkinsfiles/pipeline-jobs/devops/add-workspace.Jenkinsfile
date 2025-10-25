@Library("jenkins-sqlite-lib") _
pipeline {
    agent {
        label "built-in"
    }
    parameters {
        string(name: "ws", defaultValue: "ws-1")
        string(name: "asdb-version", defaultValue: "8.0.0.4")
        string(name: "asdb-size", defaultValue: "3")
        string(name: "backup-service-version", defaultValue: "v3")
        string(name: "storage-provider", defaultValue: "gcp")
        string(name: "infra-branch", defaultValue: "master")
    }
    stages {
        stage("Add Workspace") {
            steps {
                script {
                    AddWorkspace([
                        "ws": params['ws'],
                        "asdb-version": params['asdb-version'],
                        "asdb-size": params['asdb-size'],
                        "backup-service-version": params['backup-service-version'],
                        "storage-provider": params['storage-provider'],
                        "infra-branch": params['infra-branch']
                    ])
                }
            }
        }
    }
}

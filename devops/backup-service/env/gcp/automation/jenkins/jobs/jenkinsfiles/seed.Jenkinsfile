pipeline {
    agent any
    stages {
        stage('Checkout') {
            steps {
            git branch: "master",
            changelog: false,
            credentialsId: "github-token",
            poll: false,
            url: "https://github.com/citrusleaf/ecosystem-automation.git"
            }

        }
        stage('Seed Multi Branch Pipeline Jobs') {
            steps {
                script {
                    jenkinsfiles = findFiles(glob: '**/multi-branch-pipeline-jobs/**/*.Jenkinsfile')
                    jobDsl(targets: "devops/backup-service/env/gcp/automation/jenkins/jobs/multi_branch_pipeline_seed.groovy",
                    additionalParameters: [jenkinsfiles: jenkinsfiles])
                }
            }
        }
        stage('Seed Pipeline Jobs') {
            steps {
                script {
                    jenkinsfiles = findFiles(glob: '**/pipeline-jobs/**/*.Jenkinsfile')
                    jobDsl(targets: "devops/backup-service/env/gcp/automation/jenkins/jobs/pipeline_seed.groovy",
                    additionalParameters: [jenkinsfiles: jenkinsfiles])
                }
            }
        }
    }
}

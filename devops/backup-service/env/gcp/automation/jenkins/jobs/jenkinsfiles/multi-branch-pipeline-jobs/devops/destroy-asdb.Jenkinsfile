pipeline {
    agent {
        label "jenkins-gcp-env-provisioner"
    }

    parameters {
        booleanParam(name: "debug-mode", defaultValue: false, description: "Pauses job execution after the test run for debugging purposes")
        choice(name: "ws", choices: ["ws-1", "ws-2", "ws-3"], description: "Select workspace")
    }

    options {
        timeout(time: 1, unit: "HOURS")
        buildDiscarder(logRotator(numToKeepStr: "20", daysToKeepStr: "30"))
        overrideIndexTriggers(false)
    }

    environment {
        CLOUDSDK_PYTHON_SITEPACKAGES = "1"
        GITHUB_TOKEN = credentials("github-token")
    }

    stages {
        stage("Destroy Initial Staging Environment") {
            environment {
                TF_INPUT = "0"
                TF_IN_AUTOMATION = "1"
                PATH = "/opt/python_venv/bin:${env.PATH}"
                ASDB_WORKSPACE = "${params["ws"]}"
            }
            steps {
                dir("./devops/backup-service/env/gcp/deploy-asdb") {
                    sh """
                    task destroy -- ${ASDB_WORKSPACE}
                    """
                }
            }
        }
    }

    post {
        always {
            script {
                if (params['debug-mode'] == true) {
                    echo "====== Environment Variables ======"
                    sh("printenv")
                    echo "==================================="
                    echo "Debug Mode Enabled: Job will stay idle..."
                    echo "SSH to the worker using following command:"
                    echo "gcloud compute ssh --zone me-west1-a jenkins@${NODE_NAME} --tunnel-through-iap --project ecosystem-connectors-data"
                    input(
                        message: "Job is paused for debugging. Click 'Confirm proceed' to finish.",
                        parameters: [booleanParam(name: 'confirm-proceed', defaultValue: false, description: 'Confirm proceed')]
                    )
                }
            }
            cleanWs(cleanWhenNotBuilt: false,
                    deleteDirs: true,
                    disableDeferredWipeout: true,
                    notFailBuild: true,
                    patterns: [[pattern: "**/.terragrunt-cache/", type: "INCLUDE"],
                               [pattern: "**/.terraform/", type: "INCLUDE"],
                               [pattern: "**/.terraform.lock.hcl", type: "INCLUDE"],
                               [pattern: "**/*.pem", type: "INCLUDE"],
                               [pattern: "**/*.jks", type: "INCLUDE"]])
        }
    }
}

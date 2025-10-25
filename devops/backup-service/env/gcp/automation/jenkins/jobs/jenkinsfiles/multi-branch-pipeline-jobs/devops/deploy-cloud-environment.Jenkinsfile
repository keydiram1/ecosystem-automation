pipeline {
    agent {
        label "jenkins-gcp-env-provisioner"
    }

    parameters {
        booleanParam(name: "debug-mode", defaultValue: false, description: "Pauses job execution after the test run for debugging purposes")

        string(name: "gke-cluster-name", defaultValue: "ecosys-gke")

        string(name: "gke-main-node-pool-name", defaultValue: "main-pool")
        string(name: "gke-main-node-pool-machine-type", defaultValue: "e2-standard-4")
        string(name: "gke-main-node-pool-label", defaultValue: "main-node")
        string(name: "gke-main-pool-min-size", defaultValue: "1")
        string(name: "gke-main-pool-max-size", defaultValue: "5")

        string(name: "gke-abs-node-pool-name", defaultValue: "aerospike-backup-service-pool")
        string(name: "gke-abs-node-pool-machine-type", defaultValue: "e2-highcpu-16")
        string(name: "gke-abs-node-pool-label", defaultValue: "aerospike-backup-service-node")
        string(name: "gke-abs-pool-min-size", defaultValue: "0")
        string(name: "gke-abs-pool-max-size", defaultValue: "5")

        string(name: "gke-asa-node-pool-name", defaultValue: "aerospike-secret-agent-pool")
        string(name: "gke-asa-node-pool-machine-type", defaultValue: "e2-small")
        string(name: "gke-asa-node-pool-label", defaultValue: "aerospike-secret-agent-node")
        string(name: "gke-asa-pool-min-size", defaultValue: "1")
        string(name: "gke-asa-pool-max-size", defaultValue: "1")

    }
    options {
        timeout(time: 1, unit: "HOURS")
        buildDiscarder(logRotator(numToKeepStr: "20", daysToKeepStr: "30"))
        overrideIndexTriggers(false)
    }

    environment {
        CLOUDSDK_PYTHON_SITEPACKAGES=1
        GITHUB_TOKEN = credentials("github-token")
    }

    stages {
        stage("Generate common_vars.yaml") {
        environment {
            PROJECT_ID = "ecosystem-connectors-data"
            BUCKET_NAME = "ecosys-workspace-vars"

            GKE_CLUSTER_NAME = "${params["gke-cluster-name"]}"

            GKE_MAIN_NODE_POOL_NAME = "${params["gke-main-node-pool-name"]}"
            GKE_MAIN_NODE_POOL_MACHINE_TYPE = "${params["gke-main-node-pool-machine-type"]}"
            GKE_MAIN_NODE_POOL_LABEL = "${params["gke-main-node-pool-label"]}"
            GKE_MAIN_POOL_MIN_SIZE = "${params["gke-main-pool-min-size"]}"
            GKE_MAIN_POOL_MAX_SIZE = "${params["gke-main-pool-max-size"]}"

            GKE_ABS_NODE_POOL_NAME = "${params["gke-abs-node-pool-name"]}"
            GKE_ABS_NODE_POOL_MACHINE_TYPE = "${params["gke-abs-node-pool-machine-type"]}"
            GKE_ABS_NODE_POOL_LABEL = "${params["gke-abs-node-pool-label"]}"
            GKE_ABS_POOL_MIN_SIZE = "${params["gke-abs-pool-min-size"]}"
            GKE_ABS_POOL_MAX_SIZE = "${params["gke-abs-pool-max-size"]}"

            GKE_ASA_NODE_POOL_NAME = "${params["gke-asa-node-pool-name"]}"
            GKE_ASA_NODE_POOL_MACHINE_TYPE = "${params["gke-asa-node-pool-machine-type"]}"
            GKE_ASA_NODE_POOL_LABEL = "${params["gke-asa-node-pool-label"]}"
            GKE_ASA_POOL_MIN_SIZE = "${params["gke-asa-pool-min-size"]}"
            GKE_ASA_POOL_MAX_SIZE = "${params["gke-asa-pool-max-size"]}"
        }
            steps {
                dir("./devops/backup-service/env/gcp/cloud-env") {
                    sh 'envsubst < common_vars.tpl.yaml > common_vars.yaml'
                    sh "cat common_vars.yaml"
                }
            }
        }
        stage("Deploy Staging Environment") {
            environment {
                TF_INPUT = "0"
                TF_IN_AUTOMATION = "1"
                PATH = "/opt/python_venv/bin:${env.PATH}"
            }
            steps {
                dir("./devops/backup-service/env/gcp/cloud-env") {
                    sh """
                    task apply
                    """
                }
            }
        }
    }
    post {
        success {
            build(job: 'devops/add-cloud-environment', parameters: [
                    string(name: "gke-name", value: "${params["gke-cluster-name"]}")
                    ])
        }
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
                    parameters: [booleanParam(name: 'confirm-proceed', defaultValue: false, description: 'Confirm proceed')])
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

def absScriptPath = "/devops/backup-service/env/gcp/automation/jenkins/jobs/utils/AbsScript.groovy"
def absScript

pipeline {
    agent {
        label "jenkins-multi-node-local-tests-worker"
    }
    parameters {
        booleanParam(name: "debug-mode", defaultValue: false, description: "Pauses job execution after the test run for debugging purposes")
        choice(name: 'tests-tag', description: 'How many test classes to run in parallel',
               choices: ['ABS-E2E', 'ABS-SEQUENTIAL-TESTS', 'ABS-SEQUENTIAL-TESTS-2', 'ABS-NEGATIVE-TESTS', 'ABS-SERVICE-TEST',
                         'ABS-C-TO-GO', 'ABS-GO-TO-C', 'ABS-LONG-DURATION-TEST-LOCAL', 'ABS-LOCAL-LOAD-TEST', 'ABS-CONFIGURATIONS'])
         string(name: "abs-branch", defaultValue: "v3", description: "ABS repo branch")
         choice(name: "asdb-version", description: "Aerospike image version", choices: ["8.0.0.2", "7.2.0.3", "7.2.0.6", "7.0.0.18", "6.4.0.26", "6.3.0.31"])
        choice(name: 'storage-provider', description: 'choose storage provider', choices: ['local', 'gcp', 'azure', 'aws'])
        booleanParam(name: "reinstall-abs", defaultValue: true, description: "Reinstall ABS if true")
        booleanParam(name: "run-tests", defaultValue: true, description: "Run tests if true")
        choice(name: "jvm-suspend", description: "JVM Suspend For Remote Debug", choices: ["n", "y"])
        choice(name: "parallel-classes", description: "Number of test classes to run in parallel", choices: ["30", "1", "2", "3"])
        booleanParam(name: "pull-from-jfrog", defaultValue: false, description: "Pull images from JFrog if true")
        string(name: "abs-version", defaultValue: "v3", description: "Jfrog-ABS image tag")
        choice(name: "abs-jfrog-stage", description: "JFrog stage to pull from", choices: ["dev", "stage", "prod"])
        choice(name: "config-backup-parallel", description: "Parallelism for backup", choices: ["8", "1"])
        choice(name: "config-restore-parallel", description: "Parallelism for restore", choices: ["8", "1"])
        choice(name: "abs-configuration-file", description: "Parallelism for restore", choices: ["local", "s3", "http"])
        choice(name: "tls-enabled", description: "TLS Deployment", choices: ["true", "false"])
        choice(name: "asdb-size", description: "TLS Deployment", choices: ["1", "3"])
    }
    options {
        timeout(time: 1, unit: "HOURS")
        buildDiscarder(logRotator(numToKeepStr: "20", daysToKeepStr: "30"))
        buildBlocker(useBuildBlocker: true, blockLevel: 'NODE', scanQueueFor: 'ALL', blockingJobs: 'abs/.*')
        overrideIndexTriggers(false)
    }
    environment {
        PROJECT_ID = "ecosystem-connectors-data"
        BUCKET_NAME = "ecosys-workspace-vars"
        DOCKER_REGISTRY = "aerospike.jfrog.io"
        SERVICE_NAME = "aerospike-backup-service"
        PARALLEL_CLASSES = "${params['parallel-classes']}"
        JVM_SUSPEND = "${params['jvm-suspend']}"
        ASDB_VERSION = "${params['asdb-version']}"
        INFRA_BRANCH = "${params['infra-branch'] ?: 'main'}"
        ABS_VERSION = "${params['abs-version']}"
        ABS_BRANCH = "${params['abs-branch']}"
        ABS_JFROG_STAGE = "${params['abs-jfrog-stage']}"
        CONFIG_BACKUP_PARALLEL = "${params['config-backup-parallel']}"
        CONFIG_RESTORE_PARALLEL = "${params['config-restore-parallel']}"
        PULL_FROM_JFROG = "${params['pull-from-jfrog']}"
        TEST_TAG = "${params['tests-tag']}"
        GITHUB_TOKEN = credentials("github-token")
        AZURE_CLIENT_ID = credentials("azure-client-id")
        AZURE_STORAGE_ACCOUNT = credentials("azure-storage-account")
        AZURE_TENANT_ID = credentials("azure-tenant-id")
        AZURE_CLIENT_SECRET = credentials("azure-client-secret")
        GCP_SA_KEY_FILE = credentials("gcp-sa-key-file")
        STORAGE_PROVIDER = "${params['storage-provider']}"
        ABS_CONFIGURATION_FILE = "${params['abs-configuration-file']}"
        AWS_ACCESS_KEY_ID = credentials("aws-access-key-id")
        AWS_SECRET_ACCESS_KEY = credentials("aws-secret-access-key")
        REGISTRY = "${params['jvm-suspend']}"
        TLS_ENABLED = "${params['tls-enabled']}"
        ASDB_SIZE =  "${params['asdb-size']}"

    }

    stages {
        stage("Installing Multi-Node Environment") {
            steps {
                sh 'printenv'
                dir("./devops/env/local") {
                    ansiblePlaybook(
                        installation: 'ansible-test',
                        playbook: 'install-infra.yaml',
                        inventory: 'localhost',
                        extras: "--extra-vars '{\"aerospike\": {\"tls_enabled\": ${TLS_ENABLED}, \"size\": ${ASDB_SIZE}, \"version\": \"${ABS_VERSION}\"}}'",
                        colorized: true
                    )
                }
                dir("./devops/env/local/playbooks") {
                    ansiblePlaybook(
                        installation: 'ansible-test',
                        playbook: 'install-abs.yaml',
                        inventory: 'localhost',
                        extras: "--extra-vars '{\"aerospike\": {\"tls_enabled\": \"${params['tls-enabled']}\", \"size\": ${params['asdb-size']}, \"version\": \"${params['abs-version']}\"}}'",
                        colorized: true
                    )
                }
            }
        }
        stage ("Fetch Network Details") {
            steps {
                sh 'kubectl wait --for=condition=Ready pod -l statefulset.kubernetes.io/pod-name -n aerospike --timeout=600s'
                
                script {
                    env.SECRET_AGENT_IP = sh(
                        script: "kubectl get service istio-ingress " +
                                "--namespace istio-ingress " +
                                "-o jsonpath='{.status.loadBalancer.ingress[0].ip}'",
                        returnStdout: true
                    ).trim()

                    env.SECRET_AGENT_PORT = "3005"

                    env.ASDB_IP = sh(
                        script: "kubectl get nodes -o jsonpath='" +
                                "{range .items[*]}" +
                                "{.status.addresses[?(@.type==\"ExternalIP\")].address}" +
                                "{.status.addresses[?(@.type==\"InternalIP\")].address}{end}'",
                        returnStdout: true
                    ).trim()

                    env.ASDB_PORT = sh(
                        script: "kubectl get service " +
                                "--namespace aerospike aerocluster-0-0 " +
                                "-o jsonpath='{.spec.ports[0].nodePort}'",
                        returnStdout: true
                    ).trim()
                }
            }
        }
        stage ("Print Resources") {
            steps {
                echo "====== Connection Details ======"
                echo "SECRET_AGENT_IP: ${env.SECRET_AGENT_IP}"
                echo "SECRET_AGENT_PORT: ${env.SECRET_AGENT_PORT}"
                echo "ASDB_IP: ${env.ASDB_IP}"
                echo "ASDB_PORT: ${env.ASDB_PORT}"
                echo "================================"
                sh 'sleep 60'
                sh 'kubectl get pods -n aerospike'
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
                    parameters: [booleanParam(name: 'confirm-proceed', defaultValue: false, description: 'Confirm proceed')])
                }
            }

            dir("./devops/env/local/playbooks") {
                ansiblePlaybook(
                    installation: 'ansible-test',
                    playbook: 'uninstall-abs.yaml',
                    inventory: 'localhost',
                    extras: "--extra-vars '{\"aerospike\": {\"tls_enabled\": \"${params['tls-enabled']}\", \"size\": ${params['asdb-size']}, \"version\": \"${params['abs-version']}\"}}'",
                    colorized: true
                )
            }

            dir("./devops/env/local") {
                ansiblePlaybook(
                    installation: 'ansible-test',
                    playbook: 'uninstall-infra.yaml',
                    inventory: 'localhost',
                    extras: "--extra-vars '{\"aerospike\": {\"tls_enabled\": \"${params['tls-enabled']}\", \"size\": ${params['asdb-size']}, \"version\": \"${params['abs-version']}\"}}'",
                    colorized: true
                )
            }

            cleanWs(deleteDirs: true)
        }
    }
}

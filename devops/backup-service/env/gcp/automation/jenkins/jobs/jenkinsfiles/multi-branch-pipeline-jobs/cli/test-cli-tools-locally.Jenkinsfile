def cliToolsScriptPath = "/devops/backup-service/env/gcp/automation/jenkins/jobs/utils/CliToolsScript.groovy"
def cliToolsScript

pipeline{

    agent {
        label "jenkins-local-test-worker"
    }
parameters{
    booleanParam(name: "debug-mode", defaultValue: false, description: "Pauses job execution after the test run for debugging purposes")
    choice(name: 'tests-tag', description: 'Which test suite you want to run',
           choices: ['CLI-BACKUP', 'XDR-CLI-BACKUP', 'CLI-BACKUP-SEQUENTIAL', 'CLI-BACKUP-NEGATIVE', 'CLI-LOAD-TEST', 'CLI-XDR-LOAD-TEST', 'CLI-BACKUP-C-TO-GO', 'CLI-BACKUP-GO-TO-C'])
    booleanParam(name: 'reinstall-asbackup', defaultValue: true)
    string(name: 'cli-backup-branch', defaultValue: 'main', description: 'The asbackup branch you want to install from')
    choice(name: 'jvm-suspend', description: 'JVM Suspend For Remote Debug', choices: ['n', 'y'])
    choice(name: 'parallel-classes', description: 'How many test classes to run in parallel', choices: ['30', '1', '2', '3'])
    choice(name: 'asdb-version', description: 'Set the version of the Aerospike images', choices: ['8.0.0.8', '8.0.0.2', '8.0.0.1', '7.1.0.2', '7.0.0.3', '6.1.0.28', '6.4.0.6'])
}

options {
    timeout(time: 3, unit: 'HOURS')
    buildDiscarder(logRotator(numToKeepStr: '10', daysToKeepStr: '30'))
    overrideIndexTriggers(false)
}

environment {
    PROJECT_ID = "ecosystem-connectors-data"
    BUCKET_NAME = "ecosys-workspace-vars"
    DOCKER_REGISTRY = "aerospike.jfrog.io"
    TESTS_TAG = "${params['tests-tag']}"
    REINSTALL_ASBACKUP = "${params['reinstall-asbackup']}"
    CLI_BACKUP_BRANCH = "${params['cli-backup-branch']}"
    JVM_SUSPEND = "${params['jvm-suspend']}"
    PARALLEL_CLASSES = "${params['parallel-classes']}"
    ASDB_VERSION = "${params['asdb-version']}"
    TEST_TAG = "${params['tests-tag']}"
    GCP_SA_KEY_FILE = credentials("gcp-sa-key-file")
    AZURE_ACCOUNT_NAME = credentials("azure-storage-account")
    AZURE_ACCOUNT_KEY = credentials("azure-storage-account-key")
    AZURE_CLIENT_ID = credentials("azure-client-id")
    AZURE_STORAGE_ACCOUNT = credentials("azure-storage-account")
    AZURE_TENANT_ID = credentials("azure-tenant-id")
    AZURE_CLIENT_SECRET = credentials("azure-client-secret")
    AWS_ACCESS_KEY_ID = credentials("aws-access-key-id")
    AWS_SECRET_ACCESS_KEY = credentials("aws-secret-access-key")
    GITHUB_TOKEN = credentials("github-token")
}

    tools {
        go 'Default'
    }

    stages {
        stage("Docker Login") {
            steps {
                script {
                    withCredentials([
                        string(credentialsId: "docker-username", variable: "DOCKER_USERNAME"),
                        string(credentialsId: "docker-password", variable: "DOCKER_PASSWORD")]) {
                            sh 'echo $DOCKER_PASSWORD | docker login $DOCKER_REGISTRY -u $DOCKER_USERNAME --password-stdin'
                    }
                }
            }
        }
        stage("Uninstall asbackup") {
            when {
                expression { return params['reinstall-asbackup'] }
            }
            steps {
                script {
                    cliToolsScript = load "${pwd()}/${cliToolsScriptPath}"
                    cliToolsScript.uninstall()
                }
            }
        }
        stage('Install asbackup') {
            when {
                expression { return params['reinstall-asbackup'] }
            }
            steps {
                script {
                    cliToolsScript.install()
                }
            }
        }
        stage("Run CLI Backup Tests") {
            steps {
                withMaven(maven: 'maven-latest') {
                    withCredentials([
                        string(credentialsId: "rp-token", variable: "RP_TOKEN")
                    ]) {
                        script {
                            cliToolsScript = load "${pwd()}/${cliToolsScriptPath}"
                            cliToolsScript.runMvnIntegrationTest()
                        }
                    }
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
                    parameters: [booleanParam(name: 'confirm-proceed', defaultValue: false, description: 'Confirm proceed')])
                }
            }
            script {
                cliToolsScript = load "${pwd()}/${cliToolsScriptPath}"
                cliToolsScript.cleanAll()
            }
        }
    }
}
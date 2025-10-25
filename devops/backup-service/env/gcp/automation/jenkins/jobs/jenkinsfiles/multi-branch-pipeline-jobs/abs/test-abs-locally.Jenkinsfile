def absScriptPath = "/devops/backup-service/env/gcp/automation/jenkins/jobs/utils/AbsScript.groovy"
def absScript

pipeline {
    agent {
        label "jenkins-local-test-worker"
    }
    parameters {
        booleanParam(name: "debug-mode", defaultValue: false, description: "Pauses job execution after the test run for debugging purposes")
        choice(name: 'tests-tag', description: 'Which test suite you want to run',
               choices: ['ABS-E2E', 'ABS-SEQUENTIAL-TESTS', 'ABS-SEQUENTIAL-TESTS-2', 'ABS-NEGATIVE-TESTS', 'ABS-SERVICE-TEST',
                         'ABS-C-TO-GO', 'ABS-GO-TO-C', 'ABS-LONG-DURATION-TEST-LOCAL', 'ABS-LOCAL-LOAD-TEST', 'ABS-CONFIGURATIONS', 'DEBUG'])
         string(name: "abs-branch", defaultValue: "v3", description: "ABS repo branch")
         choice(name: "asdb-version", description: "Aerospike image version", choices: ["8.1.0.0", "8.0.0.8", "7.2.0.3", "7.2.0.6", "7.0.0.18", "6.4.0.26", "6.3.0.31"])
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
        ABS_STORAGE_PROVIDER = "${params['storage-provider']}"
        ABS_CONFIGURATION_FILE = "${params['abs-configuration-file']}"
        AWS_ACCESS_KEY_ID = credentials("aws-access-key-id")
        AWS_SECRET_ACCESS_KEY = credentials("aws-secret-access-key")
        REGISTRY = "aerospike.jfrog.io/ecosystem-dockerhub-mirror"
    }

    stages {
        stage("Uninstall ABS") {
            when {
                expression { return params['reinstall-abs'] }
            }
            steps {
                script {
                    absScript = load "${pwd()}/${absScriptPath}"
                    absScript.uninstall()
                }
            }
        }
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
        stage("Checkout and Build ABS Project") {
            when {
                environment name: "PULL_FROM_JFROG", value: "false"
            }
              steps {
                script {
                    dir (env.SERVICE_NAME) {
                        git branch: env.ABS_BRANCH,
                            changelog: false,
                            poll: false,
                            url: "https://github.com/aerospike/aerospike-backup-service"
                        echo "Building ABS Project"
                        withEnv(["IMAGE_TAG=${env.ABS_VERSION}",
                                 "REGISTRY=${env.REGISTRY}",
                                 "DOCKER_REGISTRY=${env.DOCKER_REGISTRY}"]) {
                            sh 'make docker-build'
                        }
                    }
                }
              }
        }
        stage("Pull an Image from JFrog") {
            when {
                environment name: "PULL_FROM_JFROG", value: "true"
            }
            steps {
                sh '''
                    docker pull "$DOCKER_REGISTRY/ecosystem-container-$ABS_JFROG_STAGE-local/$SERVICE_NAME:$ABS_VERSION"

                    IMAGE_ID=$(docker images \
                        --filter "reference=$DOCKER_REGISTRY/ecosystem-container-$ABS_JFROG_STAGE-local/$SERVICE_NAME:$ABS_VERSION" \
                        --format "{{.ID}}")

                    if [ -n "$IMAGE_ID" ]; then
                        docker tag "$IMAGE_ID" "aerospike/$SERVICE_NAME:$ABS_VERSION"
                    else
                        echo "Error: Image not found!"
                        exit 1
                    fi
                '''
            }
        }
        stage("Install ABS") {
            when {
                expression { return params['reinstall-abs'] }
            }
            steps {
                script {
                    absScript.install()
                    sleep 10 // Allow time for ABS to be fully ready
                }
            }
        }
        stage("Run Backup Tests") {
            steps {
                withMaven(maven: 'maven-latest') {
                    withCredentials([
                        file(credentialsId: "ca-aerospike-com-pem", variable: 'CA_AEROSPIKE_COM_PEM_PATH'),
                        file(credentialsId: "ca-aerospike-com-pem-jks", variable: 'CA_AEROSPIKE_COM_PEM_JKS_PATH'),
                        string(credentialsId: "rp-token", variable: "RP_TOKEN")
                    ]) {
                        script {
                            absScript = load "${pwd()}/${absScriptPath}"
                            absScript.runMvnIntegrationTest()
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
                absScript = load "${pwd()}/${absScriptPath}"
                absScript.cleanAll()
            }
        }
    }
}

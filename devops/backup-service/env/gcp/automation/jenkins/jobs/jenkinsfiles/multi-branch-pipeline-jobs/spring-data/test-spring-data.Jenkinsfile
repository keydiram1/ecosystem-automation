def springDataScriptPath = "/devops/backup-service/env/gcp/automation/jenkins/jobs/utils/SpringDataScript.groovy"
def springDataScript

pipeline {

    agent {
        label "jenkins-local-test-worker"
    }
    parameters {
        booleanParam(name: "debug-mode", defaultValue: false, description: "Pauses job execution after the test run for debugging purposes")
        choice(name: 'tests-tag', description: 'Which test suite you want to run',
                choices: ['SPRING-DATA-TESTS-1', 'SPRING-DATA-TESTS-2'])
        booleanParam(name: 'reinstall-spring-data', defaultValue: true)
        string(name: "spring-data-branch", defaultValue: "main", description: "Spring Data repo branch")
        string(name: 'spring-data-version', defaultValue: '5.1.1', description: 'The Spring Data version you want to test')
        choice(name: 'parallel-classes', description: 'How many test classes to run in parallel', choices: ['1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '15', '20'])
        string(name: 'aerospike-source-image-name', defaultValue: 'aerospike.jfrog.io/docker/aerospike/aerospike-server-enterprise', description: 'The name of the AS DB image')
        string(name: 'asdb-version', defaultValue: '8.0.0.7', description: 'The version of the AS DB image')
        string(name: 'notes', defaultValue: 'No Notes', description: 'Add notes that will be shown in Report Portal')
    }

    environment {
        PROJECT_ID = "ecosystem-connectors-data"
        BUCKET_NAME = "ecosys-workspace-vars"
        DOCKER_REGISTRY = "aerospike.jfrog.io"
        REINSTALL_SPRING_DATA = "${params['reinstall-spring-data']}"
        SPRING_DATA_VERSION = "${params['spring-data-version']}"
        PARALLEL_CLASSES = "${params['parallel-classes']}"
        ASDB_VERSION = "${params['asdb-version']}"
        NOTES = "${params['notes']}"
        AEROSPIKE_IMAGE_NAME = "${params['aerospike-source-image-name']}"
        SPRING_DATA_BRANCH = "${params['spring-data-branch']}"
        TESTS_TAG = "${params['tests-tag']}"
    }
    
    options {
        timeout(time: 1, unit: 'HOURS')
        buildDiscarder(logRotator(numToKeepStr: '10', daysToKeepStr: '30'))
        overrideIndexTriggers(false)
    }

    stages {
        stage('Uninstall Spring Data') {
            when {
                expression { return params['reinstall-spring-data'] }
            }
            steps {
                script {
                    springDataScript = load "${pwd()}/${springDataScriptPath}"
                    springDataScript.uninstall()
                }
                script {
                    sh '''
                        docker logout "$DOCKER_REGISTRY" || true
                        docker stop $(docker ps -aq) || true
                        docker rm $(docker ps -aq) || true
                        docker rmi $(docker images -q) || true
                    '''
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
        stage('Install Spring Data') {
            when {
                expression { return params['reinstall-spring-data'] }
            }
            steps {
                withMaven(maven: 'maven-latest') {
                    withCredentials([
                        string(credentialsId: "rp-token", variable: "RP_TOKEN")
                    ]) {
                        script {
                            springDataScript.install()
                        }
                    }
                }
            }
        }

        stage('Run Spring Data tests') {
            steps {
                withMaven(maven: 'maven-latest') {
                    withCredentials([
                        string(credentialsId: "rp-token", variable: "RP_TOKEN")
                    ]) {
                        script {
                            springDataScript = load "${pwd()}/${springDataScriptPath}"
                            springDataScript.runMvnIntegrationTest()
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
                springDataScript = load "${pwd()}/${springDataScriptPath}"
                springDataScript.cleanAll()
            }
        }
    }
}
def cliToolsScriptPath = "/devops/backup-service/env/gcp/automation/jenkins/jobs/utils/CliToolsScript.groovy"
def cliToolsScript

pipeline{

    agent {
        label "jenkins-local-test-worker"
    }
parameters{
    choice(name: 'tests-tag', description: 'Which test suite you want to run',
           choices: ['XDR-CLI-BACKUP', 'CLI-BACKUP', 'CLI-BACKUP-SEQUENTIAL', 'CLI-BACKUP-NEGATIVE', 'CLI-LOAD-TEST', 'CLI-XDR-LOAD-TEST', 'CLI-BACKUP-C-TO-GO', 'CLI-BACKUP-GO-TO-C'])
    booleanParam(name: 'reinstall-asbackup', defaultValue: true)
    string(name: 'cli-backup-branch', defaultValue: 'main', description: 'The asbackup branch you want to install from')
    choice(name: 'jvm-suspend', description: 'JVM Suspend For Remote Debug', choices: ['n', 'y'])
    choice(name: 'parallel-classes', description: 'How many test classes to run in parallel', choices: ['30', '1', '2', '3'])
    choice(name: 'asdb-version', description: 'Set the version of the Aerospike images', choices: ['8.0.0.0-rc2', '8.0.0.1', '7.1.0.2', '7.0.0.3', '6.1.0.28', '6.4.0.6'])
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
}
    stages {
        stage('Install multi node cluster') {
            when {
                expression { return params['reinstall-asbackup'] }
            }
            steps {
                dir("./devops/env/local") {
                    ansiblePlaybook(
                            installation: 'ansible',
                            playbook: 'install-secret-agent.yaml',
                            inventory: 'localhost,',
                            extraVars: [
                                    'aerospike.tls_enabled': "${params['tls-enabled']}",
                                    'aerospike.size'       : "${params['asdb-size']}",
                                    'aerospike.version'    : "${params['abs-version']}"
                            ],
                            colorized: true)
                    ansiblePlaybook(
                            installation: 'ansible',
                            playbook: 'install-asdb.yaml',
                            inventory: 'localhost,',
                            extraVars: [
                                    'aerospike.tls_enabled': "${params['tls-enabled']}",
                                    'aerospike.size'       : "${params['asdb-size']}",
                                    'aerospike.version'    : "${params['abs-version']}"
                            ],
                            colorized: true)
                    ansiblePlaybook(
                            installation: 'ansible',
                            playbook: 'install-abs.yaml',
                            inventory: 'localhost,',
                            extraVars: [
                                    'aerospike.tls_enabled': "${params['tls-enabled']}",
                                    'aerospike.size'       : "${params['asdb-size']}",
                                    'aerospike.version'    : "${params['abs-version']}"
                            ],
                            colorized: true)

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
                        file(credentialsId: "ca-aerospike-com-pem", variable: 'CA_AEROSPIKE_COM_PEM_PATH'),
                        file(credentialsId: "ca-aerospike-com-pem-jks", variable: 'CA_AEROSPIKE_COM_PEM_JKS_PATH'),
                        string(credentialsId: "rp-token", variable: "RP_TOKEN")
                    ]) {
                        cliToolsScript = load "${pwd()}/${cliToolsScriptPath}"
                        cliToolsScript.runMvnIntegrationTest()
                    }
                }
            }
        }

        stage("Uninstall asbackup") {
            when {
                expression { return params['reinstall-asbackup'] }
            }
            steps {
                dir("./devops/env/local") {
                    ansiblePlaybook(
                            installation: 'ansible',
                            playbook: 'uninstall-abs.yaml',
                            inventory: 'localhost,',
                            extraVars: [
                                    'aerospike.tls_enabled': "${params['tls-enabled']}",
                                    'aerospike.size'       : "${params['asdb-size']}",
                                    'aerospike.version'    : "${params['abs-version']}"
                            ],
                            colorized: true)
                    ansiblePlaybook(
                            installation: 'ansible',
                            playbook: 'uninstall-asdb.yaml',
                            inventory: 'localhost,',
                            extraVars: [
                                    'aerospike.tls_enabled': "${params['tls-enabled']}",
                                    'aerospike.size'       : "${params['asdb-size']}",
                                    'aerospike.version'    : "${params['abs-version']}"
                            ],
                            colorized: true)
                    ansiblePlaybook(
                            installation: 'ansible',
                            playbook: 'uninstall-secret-agent.yaml',
                            inventory: 'localhost,',
                            extraVars: [
                                    'aerospike.tls_enabled': "${params['tls-enabled']}",
                                    'aerospike.size'       : "${params['asdb-size']}",
                                    'aerospike.version'    : "${params['abs-version']}"
                            ],
                            colorized: true)
                }
            }
        }
    }
    post {
        always {
            cleanWs(cleanWhenNotBuilt: true,
                    deleteDirs: true,
                    disableDeferredWipeout: true,
                    notFailBuild: true)
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
}
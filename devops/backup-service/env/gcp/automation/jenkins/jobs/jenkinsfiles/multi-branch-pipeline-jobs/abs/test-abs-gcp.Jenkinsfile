def absScriptPath = "/devops/backup-service/env/gcp/automation/jenkins/jobs/utils/AbsScript.groovy"
def absScript

properties([
        parameters([
                booleanParam(name: "debug-mode", defaultValue: false, description: "Pauses job execution after the test run for debugging purposes"),
                choiceParam(name: "ws", description: "Select workspace", choices: ["ws-1", "ws-2", "ws-3"]),
                [$class: 'CascadeChoiceParameter',
                 choiceType: 'PT_CHECKBOX',
                 description: 'Select multiple options',
                 filterLength: 1,
                 filterable: false,
                 name: 'test-tags',
                 script: [
                         $class: 'GroovyScript',
                         fallbackScript: [
                                 classpath: [],
                                 sandbox: false,
                                 script: "return ['ABS-E2E']"
                         ],
                         script: [
                                 classpath: [],
                                 sandbox: false,
                                 script: '''
                        return [
                            "ABS-E2E",
                            "ABS-NEGATIVE-TESTS",
                            "ABS-SEQUENTIAL-TESTS",
                            "ABS-SEQUENTIAL-TESTS-2",
                            "ABS-LOAD-TEST",
                            "ABS-LONG-DURATION-TEST",
                            "ABS-PERFORMANCE-TEST",
                            "DEBUG"
                        ]
                    '''
                         ]
                 ]
                ],
                choiceParam(name: "parallel-classes", description: "How many test classes to run in parallel", choices: ["30", "1", "2", "3"]),
                choiceParam(name: 'load-level', description: 'Choose the load level of the test', choices: ['low', 'high']),
                choiceParam(name: 'test-duration', description: 'Choose the test duration', choices: ['short', 'long']),
                choiceParam(name: 'on-going-throughput', description: 'On going throughput of asbench', choices: ['10', '50', '100', '500', '1000', '2000', '3000']),
                choiceParam(name: 'data-spikes-duration', description: 'Spikes duration of asbench', choices: ['1', '10', '20', '30', '60', '80', '100']),
                choiceParam(name: "jvm-suspend", description: "JVM Suspend For Remote Debug", choices: ["n", "y"]),

                choiceParam(name: "create-data", description: "Whether to create data before running the performance test", choices: ["n", "y"]),
                choiceParam(name: "number-of-records-in-millions", description: "Number of records to create (in millions) at the performance test", choices: ["1", "2", "3", "5", "10", "15", "20", "25", "30", "50", "100", "1000"]),
                choiceParam(name: "data-type", description: "Data type for performance test", choices: ["SCALAR_1KB", "COMPLEX_1KB", "MIXED_1KB", "SCALAR_3KB", "COMPLEX_3KB", "MIXED_3KB", "SCALAR_100KB", "COMPLEX_100KB", "MIXED_100KB"]),

                choiceParam(name: "truncate-data", description: "Whether to truncate data before creating new data", choices: ["n", "y"]),

                choiceParam(name: "backup-parallel", description: "Number of parallel scan threads", choices: ["8", "1", "2", "4", "16", "32"]),
                choiceParam(name: "backup-records-per-second", description: "Records per second limit (0 = unlimited)", choices: ["0", "10000", "50000", "100000", "500000"]),
                choiceParam(name: "backup-bandwidth", description: "Bandwidth limit in MiB/s (0 = unlimited)", choices: ["0", "100", "200", "500", "1000"]),
                choiceParam(name: "backup-socket-timeout", description: "Socket timeout in ms (0 = use total-timeout)", choices: ["60000000", "30000000", "90000000", "0"]),
                choiceParam(name: "backup-total-timeout", description: "Total timeout in ms (0 = no limit)", choices: ["0", "30000000", "60000000", "90000000"]),
                choiceParam(name: "backup-file-limit", description: "Limit the backup file size(mb)", choices: ["250", "100", "400", "550"]),

                choiceParam(name: "restore-parallel", description: "Number of concurrent record readers", choices: ["8", "4", "16", "32"]),
                choiceParam(name: "restore-max-async-batches", description: "Number of concurrent async batch writes", choices: ["8", "4", "16", "32"]),
                choiceParam(name: "restore-batch-size", description: "Number of records per async batch", choices: ["128", "256", "512", "1024"]),
                choiceParam(name: "restore-socket-timeout", description: "Socket timeout in ms (0 = use total-timeout)", choices: ["10000", "30000", "60000", "0"]),
                choiceParam(name: "restore-total-timeout", description: "Total timeout in ms (0 = no limit)", choices: ["60000", "120000", "300000", "600000", "0"])
        ])
])

pipeline {
    agent { label "jenkins-gcp-test-worker-svc" }

    options {
        timeout(time: 26, unit: "HOURS") // due to long duration test
        buildDiscarder(logRotator(numToKeepStr: "20", daysToKeepStr: "60"))
        overrideIndexTriggers(false)
    }

    environment {
        CLOUDSDK_PYTHON_SITEPACKAGES = 1
        GITHUB_TOKEN = credentials("github-token")
        PROJECT_ID = "ecosystem-connectors-data"
        ZONE = "me-west1-a"
        STORAGE_PROVIDER = "${params['storage-provider']}"

        ASDB_NODE = "${params['ws']}-asdb-node-0"
        TEST_TAGS = "${params['tests-tags']}"
        LOAD_LEVEL = "${params['load-level']}"
        TEST_DURATION = "${params['test-duration']}"
        ON_GOING_THROUGHPUT = "${params['on-going-throughput']}"
        DATA_SPIKES_DURATION = "${params['data-spikes-duration']}"
        AZURE_ACCOUNT_NAME = credentials("azure-storage-account")
        AZURE_ACCOUNT_KEY = credentials("azure-storage-account-key")
        AZURE_CLIENT_ID = credentials("azure-client-id")
        AZURE_STORAGE_ACCOUNT = credentials("azure-storage-account")
        AZURE_TENANT_ID = credentials("azure-tenant-id")
        AZURE_CLIENT_SECRET = credentials("azure-client-secret")
        GCP_SA_KEY_FILE = credentials("gcp-sa-key-file")
        AWS_ACCESS_KEY_ID = credentials("aws-access-key-id")
        AWS_SECRET_ACCESS_KEY = credentials("aws-secret-access-key")
        ASDB_DNS_NAME = "asd.${params['ws']}.ecosys.internal"
        ABS_DNS_NAME = "gateway.ecosys.internal/${params['ws']}/"
        PATH = "/opt/python_venv/bin:${env.PATH}"
        ENV_WORKSPACE = "${params['ws']}"

        CREATE_DATA = "${params['create-data']}"
        NUMBER_OF_RECORDS_IN_MILLIONS = "${params['number-of-records-in-millions']}"
        DATA_TYPE = "${params['data-type']}"
        TRUNCATE_DATA = "${params['truncate-data']}"

        BACKUP_PARALLEL = "${params['backup-parallel']}"
        BACKUP_RECORDS_PER_SECOND = "${params['backup-records-per-second']}"
        BACKUP_BANDWIDTH = "${params['backup-bandwidth']}"
        BACKUP_SOCKET_TIMEOUT = "${params['backup-socket-timeout']}"
        BACKUP_TOTAL_TIMEOUT = "${params['backup-total-timeout']}"
        BACKUP_FILE_LIMIT = "${params['backup-file-limit']}"

        RESTORE_PARALLEL = "${params['restore-parallel']}"
        RESTORE_MAX_ASYNC_BATCHES = "${params['restore-max-async-batches']}"
        RESTORE_BATCH_SIZE = "${params['restore-batch-size']}"
        RESTORE_SOCKET_TIMEOUT = "${params['restore-socket-timeout']}"
        RESTORE_TOTAL_TIMEOUT = "${params['restore-total-timeout']}"
    }

    stages {
        stage("Configure k8s Context") {
            steps {
                sh "gcloud container clusters get-credentials ecosys-gke --zone ${ZONE} --project ${PROJECT_ID}"
            }
        }

        stage ("Get Env Variables") {
            steps {
                dir("./devops/backup-service/env/gcp/scripts") {
                    sh "./set-env-vars.py --workspace ${params['ws']}"
                    script {
                        readProperties(file: "env.properties").each { key, value -> env[key] = value }
                    }
                }
            }
        }

        stage ("Run Cloud Tests") {
            steps {
                script {
                    echo "env: ${env.ABS_STORAGE_PROVIDER}"
                    def selected_test_tags = params['test-tags'] ? params['test-tags'].tokenize(',') : []
                    absScript = load("${pwd()}/${absScriptPath}")
                    env.PARALLEL_CLASSES = params['parallel-classes']
                    env.JVM_SUSPEND = params['jvm-suspend']

                    for (test_tag in selected_test_tags) {
                        withMaven(maven: 'maven-latest') {
                            withCredentials([
                                    file(credentialsId: "ca-aerospike-com-pem", variable: 'CA_AEROSPIKE_COM_PEM_PATH'),
                                    file(credentialsId: "ca-aerospike-com-pem-jks", variable: 'CA_AEROSPIKE_COM_PEM_JKS_PATH'),
                                    string(credentialsId: "rp-token", variable: "RP_TOKEN")
                            ]) {
                                absScript.runMvnIntegrationTestGCP(test_tag)
                            }
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
            cleanWs(cleanWhenNotBuilt: false, deleteDirs: true, disableDeferredWipeout: true, notFailBuild: true, patterns: [
                    [pattern: "**/.terragrunt-cache/", type: "INCLUDE"],
                    [pattern: "**/.terraform/", type: "INCLUDE"],
                    [pattern: "**/.terraform.lock.hcl", type: "INCLUDE"],
                    [pattern: "**/*.pem", type: "INCLUDE"],
                    [pattern: "**/*.jks", type: "INCLUDE"]
            ])
        }
    }
}

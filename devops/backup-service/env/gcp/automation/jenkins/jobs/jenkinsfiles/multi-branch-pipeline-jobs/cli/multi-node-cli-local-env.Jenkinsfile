def cliToolsScriptPath = "/devops/backup-service/env/gcp/automation/jenkins/jobs/utils/CliToolsScript.groovy"
def cliToolsScript

properties([
    parameters([
        booleanParam(name: "debug-mode", defaultValue: false, description: "Pauses job execution after the test run for debugging purposes"),
        [$class: 'CascadeChoiceParameter',
            choiceType: 'PT_CHECKBOX',
            description: 'Select Test Tag',
            referencedParameters: 'node-label',
            filterLength: 1,
            filterable: false,
            name: 'test-tags',
            script: [
                $class: 'GroovyScript',
                fallbackScript: [
                    classpath: [],
                    sandbox: false,
                    script: 'return ["BACKUP-TLS-ENV"]'
                ],
                script: [
                    classpath: [],
                    sandbox: false,
                    script: '''
                        String nodeLabel = binding.variables.get("node-label")

                        if (nodeLabel == "jenkins-multi-node-local-tests-worker-load-test") {
                            return ["CLI-LOAD-TEST", "CLI-XDR-LOAD-TEST"]
                        } else {
                            return [
                                "BACKUP-TLS-ENV",
                                "CLI-BACKUP",
                                "XDR-CLI-BACKUP",
                                "CLI-BACKUP-SEQUENTIAL",
                                "CLI-BACKUP-NEGATIVE",
                                "CLI-3-NODES-CLUSTER",
                                "DEBUG"
                                ]
                        }
                    '''
                ]
            ]
        ],
        stringParam(name: 'cli-backup-branch', defaultValue: 'main', description: 'The asbackup branch you want to install from'),
        choiceParam(name: 'jvm-suspend', description: 'JVM Suspend For Remote Debug', choices: ['n', 'y']),
        choiceParam(name: 'parallel-classes', description: 'How many test classes to run in parallel', choices: ['30', '1', '2', '3']),
        [$class: 'ChoiceParameter',
            choiceType: 'PT_SINGLE_SELECT',
            description: 'Select ASDB Version',
            filterLength: 1,
            filterable: false,
            name: 'asdb-version',
            script: [
                $class: 'GroovyScript',
                fallbackScript: [
                    classpath: [],
                    sandbox: false,
                    script: 'return ["8.0.0.9", "7.2.0.6", "7.2.0.3", "7.0.0.18", "6.4.0.26", "6.3.0.31"]'
                ],
                script: [
                    classpath: [],
                    sandbox: false,
                    script: '''
                        import groovy.sql.Sql
                        import java.sql.Driver
                        import java.util.Properties
                        import java.sql.Connection

                        List<String> versions = []
                        Properties props = new Properties()
                        String connectionUrl = "jdbc:sqlite:/data/jenkins.db"

                        Connection conn = null
                        try {
                            Driver driver = Class.forName('org.sqlite.JDBC').newInstance() as Driver
                            conn = driver.createConnection(connectionUrl, props)
                            Sql sql = new Sql(conn)

                            sql.eachRow("SELECT version FROM asdb_dockerhub_versions") { row ->
                                versions << row.version
                            }

                            sql.close()
                        } catch (Exception e) {
                            return ["Error fetching versions: ${e.message}"]
                        } finally {
                            if (conn != null) {
                                try {
                                    conn.close()
                                } catch (Exception ignored) {}
                            }
                        }
                        return [ "8.0.0.9" ] + versions.findAll { it != "8.0.0.9" }
                    '''
                ]
            ]
        ],
        choiceParam(name: "asdb-size", choices: ["3", "1"], description: "Select ASDB Size"),
        booleanParam(name: "asdb-tls-enabled", defaultValue: false, description: "Select TLS"),
        booleanParam(name: "asdb-sc-enabled", defaultValue: true, description: "Select Strong Consistency"),
        [$class: 'CascadeChoiceParameter',
            choiceType: 'PT_SINGLE_SELECT',
            description: 'Select Rooster',
            referencedParameters: 'asdb-sc-enabled',
            filterLength: 1,
            filterable: false,
            name: 'asdb-roster-enabled',
            script: [
                $class: 'GroovyScript',
                fallbackScript: [
                    classpath: [],
                    sandbox: false,
                    script: 'return ["false"]'
                ],
                script: [
                    classpath: [],
                    sandbox: false,
                    script: '''
                        Boolean asdbScEnabled = binding.variables.get("asdb-sc-enabled")
                        if (asdbScEnabled) {
                            return ["true", "false"]
                        } else {
                            return ["false"]
                        }
                    '''
                ]
            ]
        ],
        booleanParam(name: "ops-agent", description: "Select Ops Agent")
    ])
])

pipeline {
    agent {
        label "jenkins-multi-node-local-tests-worker-cli"
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
        TEST_TAG = "${params['tests-tag']}"
        REINSTALL_ASBACKUP = "${params['reinstall-asbackup']}"
        CLI_BACKUP_BRANCH = "${params['cli-backup-branch']}"
        JVM_SUSPEND = "${params['jvm-suspend']}"
        PARALLEL_CLASSES = "${params['parallel-classes']}"
        GCP_SA_KEY_FILE = credentials("gcp-sa-key-file")
        AZURE_ACCOUNT_NAME = credentials("azure-storage-account")
        AZURE_ACCOUNT_KEY = credentials("azure-storage-account-key")
        AZURE_CLIENT_ID = credentials("azure-client-id")
        AZURE_STORAGE_ACCOUNT = credentials("azure-storage-account")
        AZURE_TENANT_ID = credentials("azure-tenant-id")
        AZURE_CLIENT_SECRET = credentials("azure-client-secret")
        AWS_ACCESS_KEY_ID = credentials("aws-access-key-id")
        AWS_SECRET_ACCESS_KEY = credentials("aws-secret-access-key")
        REGISTRY = "${params['jvm-suspend']}"
        ASDB_TLS_ENABLED = "${params['asdb-tls-enabled']}"
        ASDB_ROSTER_ENABLED = "${params['asdb-roster-enabled']}"
        ASDB_SC_ENABLED = "${params['asdb-sc-enabled']}"
        ASDB_SIZE =  "${params['asdb-size']}"
        ASDB_VERSION = "${params['asdb-version']}"
        OPS_AGENT = "${params['ops-agent']}"
        GITHUB_TOKEN = credentials("github-token")
        SERVICE_NAME = "aerospike-backup-service"
        ASDB_NAMESPACES = 20
    }

    tools {
        go 'Default'
    }

    stages {
        stage("Google Ops Agent") {
            when {
                environment name: 'OPS_AGENT', value: 'true'
            }
            steps {
                 sh "sudo systemctl start google-cloud-ops-agent"
            }
        }
        stage("Installing Multi-Node Environment") {
            steps {
                withCredentials([
                    file(credentialsId: "ca-aerospike-com-pem", variable: 'CA_AEROSPIKE_COM_PEM_PATH')
                ]) {
                    script {
                        cliToolsScript = load "${pwd()}/${cliToolsScriptPath}"
                        cliToolsScript.installMultiNodeENV()
                    }
                }
            }
        }

        stage("Fetch Network Details") {
            steps {
                script {
                    cliToolsScript = load "${pwd()}/${cliToolsScriptPath}"
                    cliToolsScript.fetchNetworkDetails()
                }
            }
        }

        stage("Print Resources") {
            steps {
                echo "====== Connection Details ======"
                echo "SECRET_AGENT_IP: ${env.SECRET_AGENT_IP}"
                echo "SECRET_AGENT_PORT: ${env.SECRET_AGENT_PORT}"
                echo "ASDB_IP: ${env.ASDB_IP}"
                echo "ASDB_PORT: ${env.ASDB_PORT}"
                echo "================================"
                sh 'kubectl get pods -n aerospike'
            }
        }

        stage("Run CLI Backup Tests") {
            steps {
                script {
                    def selected_test_tags = params['test-tags'] ? params['test-tags'].tokenize(',') : []
                    cliToolsScript = load "${pwd()}/${cliToolsScriptPath}"

                    for (test_tag in selected_test_tags) {
                        withMaven(maven: 'maven-latest') {
                            withCredentials([
                                file(credentialsId: "ca-aerospike-com-pem", variable: 'CA_AEROSPIKE_COM_PEM_PATH'),
                                file(credentialsId: "ca-aerospike-com-pem-jks", variable: 'CA_AEROSPIKE_COM_PEM_JKS_PATH'),
                                string(credentialsId: "rp-token", variable: "RP_TOKEN")
                            ]) {
                                cliToolsScript.runTestOnLocalTlsEnv(test_tag)
                            }
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            withCredentials([
                file(credentialsId: "ca-aerospike-com-pem", variable: 'CA_AEROSPIKE_COM_PEM_PATH'),
                file(credentialsId: "ca-aerospike-com-pem-jks", variable: 'CA_AEROSPIKE_COM_PEM_JKS_PATH'),
                string(credentialsId: "rp-token", variable: "RP_TOKEN")
            ]) {
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
            }

            script {
                cliToolsScript = load "${pwd()}/${cliToolsScriptPath}"
                cliToolsScript.removeMultiNodeENV()
            }

            script {
                if (params['ops-agent'] == "true") {
                    sh("sudo systemctl stop google-cloud-ops-agent")
                }
            }

        }
    }
}

properties([
    parameters([
        booleanParam(
            name: "debug-mode",
            defaultValue: false,
            description: "Pauses job execution after the test run for debugging purposes"
        ),
        choiceParam(
            name: "ws",
            choices: ["ws-1", "ws-2", "ws-3"],
            description: "Select workspace"
        ),
        choiceParam(
            name: "worker-machine-type",
            choices: ["n2d-standard-8", "n2d-standard-4" ,"n2d-standard-16", "n2-standard-16"],
            description: "Select Worker Machine Type"
        ),
        [
            $class: 'DynamicReferenceParameter',
            choiceType: 'ET_FORMATTED_HTML',
            name: 'worker-devices',
            description: 'Select number of attached SSD devices',
            referencedParameters: 'worker-machine-type',
            omitValueField: true,
            script: [
                $class: 'GroovyScript',
                fallbackScript: [
                    classpath: [],
                    sandbox: false,
                    script: 'return ["Error message"]'
                ],
                script: [
                    classpath: [],
                    sandbox: false,
                    script: """
                        String machineType = binding.variables.get("asdb-machine-type")
                        return "<select name=\\"value\\" class=\\"setting-input\\" required>" +
                                "<option value=\\"4\\">4</option>" +
                                "<option value=\\"0\\">0</option>" +
                                "<option value=\\"1\\">1</option>" +
                                "<option value=\\"2\\">2</option>" +
                                "<option value=\\"8\\">8</option>" +
                                "<option value=\\"16\\">16</option>" +
                                "<option value=\\"24\\">24</option>" +
                                "</select>"
                    """
                ]
            ]
        ],
        booleanParam(name: "ops-agent", defaultValue: false, description: "Select Ops Agent"),
        booleanParam(name: "clone-backup-cli-repo", defaultValue: false, description: "Clone Aerospike Backup CLI Repo"),
        booleanParam(name: "clone-backup-service-repo", defaultValue: false, description: "Clone Aerospike Backup Service Repo"),
        booleanParam(name: "clone-backup-library-repo", defaultValue: false, description: "Clone Aerospike Backup Library Repo")
    ])
])

pipeline {
    agent {
        label "jenkins-gcp-env-provisioner"
    }

    options {
        timeout(time: 1, unit: "HOURS")
        buildDiscarder(logRotator(numToKeepStr: "20", daysToKeepStr: "30"))
        overrideIndexTriggers(false)
    }

    environment {
        CLOUDSDK_PYTHON_SITEPACKAGES = 1
        GITHUB_TOKEN                 = credentials("github-token")
    }

    tools {
        ansible 'ansible-test'
    }

    stages {
        stage("Generate common_vars.yaml") {
            environment {
                PROJECT_ID         = "ecosystem-connectors-data"
                DEBUG_MODE         = "${params['debug-mode']}"
                WORKER_WORKSPACE          = "${params['ws']}"
                WORKER_MACHINE_TYPE = "${params['worker-machine-type']}"
                WORKER_DEVICES = "${params['worker-devices']}"
                WORKER_OPS_AGENT          = "${params['ops-agent']}"
                WORKER_CLONE_BACKUP_CLI_REPO          = "${params['clone-backup-cli-repo']}"
                WORKER_CLONE_BACKUP_SERVICE_REPO = "${params['clone-backup-service-repo']}"
                WORKER_CLONE_BACKUP_LIBRARY_REPO = "${params['clone-backup-library-repo']}"

            }
            steps {
                dir("./devops/backup-service/env/gcp/deploy-worker") {
                    sh '''
                        envsubst < common_vars.tpl.yaml > common_vars.yaml
                        cat common_vars.yaml
                    '''
                }
            }
        }

        stage("Deploy Initial Staging Environment") {
            environment {
                TF_INPUT         = "0"
                TF_IN_AUTOMATION = "1"
                PATH             = "/opt/python_venv/bin:${env.PATH}"
            }
            steps {
                dir("./devops/backup-service/env/gcp/deploy-worker") {
                    sh '''
                        task apply
                    '''
                }
            }
        }
    }

    post {
        always {
            script {
                if (params['debug-mode'] == true) {
                    echo "====== Environment Variables ======"
                    sh "printenv"
                    echo "==================================="
                    echo "Debug Mode Enabled: Job will stay idle..."
                    echo "SSH to the worker using the following command:"
                    echo "gcloud compute ssh --zone me-west1-a jenkins@${NODE_NAME} --tunnel-through-iap --project ecosystem-connectors-data"
                    input(
                        message: "Job is paused for debugging. Click 'Confirm proceed' to finish.",
                        parameters: [
                            booleanParam(name: 'confirm-proceed', defaultValue: false, description: 'Confirm proceed')
                        ]
                    )
                }
            }
            cleanWs(
                cleanWhenNotBuilt: false,
                deleteDirs: true,
                disableDeferredWipeout: true,
                notFailBuild: true,
                patterns: [
                    [pattern: "**/.terragrunt-cache/",     type: "INCLUDE"],
                    [pattern: "**/.terraform/",            type: "INCLUDE"],
                    [pattern: "**/.terraform.lock.hcl",    type: "INCLUDE"],
                    [pattern: "**/*.pem",                  type: "INCLUDE"],
                    [pattern: "**/*.jks",                  type: "INCLUDE"]
                ]
            )
        }
    }
}


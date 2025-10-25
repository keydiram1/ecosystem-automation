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
        [
            $class: 'ChoiceParameter',
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
                    script: '''<your multi-line SQL fetching logic here>'''
                ]
            ]
        ],
        [
            $class: 'CascadeChoiceParameter',
            choiceType: 'PT_SINGLE_SELECT',
            description: 'Select ASDB Distro',
            referencedParameters: 'asdb-version',
            filterLength: 1,
            filterable: false,
            name: 'asdb-distro',
            script: [
                $class: 'GroovyScript',
                fallbackScript: [
                    classpath: [],
                    sandbox: false,
                    script: 'return ["ubuntu22.04"]'
                ],
                script: [
                    classpath: [],
                    sandbox: false,
                    script: '''<distro fetch script here>'''
                ]
            ]
        ],
        [
            $class: 'CascadeChoiceParameter',
            choiceType: 'PT_SINGLE_SELECT',
            description: 'Select ASDB Arch',
            referencedParameters: 'asdb-version',
            filterLength: 1,
            filterable: false,
            name: 'asdb-archs',
            script: [
                $class: 'GroovyScript',
                fallbackScript: [
                    classpath: [],
                    sandbox: false,
                    script: 'return ["amd64"]'
                ],
                script: [
                    classpath: [],
                    sandbox: false,
                    script: '''<archs fetch script here>'''
                ]
            ]
        ],
        choiceParam(
            name: "asdb-size",
            choices: ["1", "3", "7"],
            description: "Select ASDB Size"
        ),
        choiceParam(
            name: "asdb-machine-type",
            choices: ["n2-standard-4", "n2-standard-8", "n2-standard-16"],
            description: "Select ASDB Machine Type"
        ),
        stringParam(
            name: "abs-image-tag",
            defaultValue: "v3",
            description: "Aerospike Backup Service image tag"
        ),
        choiceParam(
            name: "abs-storage-provider",
            description: "Choose storage provider",
            choices: ["gcp", "azure", "aws", "minio", "local"]
        ),
        choiceParam(
            name: "asdb-device-type",
            choices: ["filesystem", "raw"],
            description: "Select ASDB Local SSD Device Type"
        ),
        [
            $class: 'DynamicReferenceParameter',
            choiceType: 'ET_FORMATTED_HTML',
            description: 'Enter the App Name to be created',
            name: 'asdb-namespaces',
            omitValueField: true,
            referencedParameters: 'asdb-device-type',
            script: [
                $class: 'GroovyScript',
                fallbackScript: [
                    classpath: [],
                    sandbox: true,
                    script: 'return ["Error message"]'
                ],
                script: [
                    classpath: [],
                    sandbox: true,
                    script: """<dynamic input field HTML logic here>"""
                ]
            ]
        ]
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
        CLOUDSDK_PYTHON_SITEPACKAGES        = 1
        GITHUB_TOKEN                        = credentials('github-token')
        PROJECT_ID                          = 'ecosystem-connectors-data'
        BUCKET_NAME                         = 'ecosys-workspace-vars'
        DEBUG_MODE                          = "${params['debug-mode']}"
        WORKSPACE                           = "${params['ws']}"
        ASDB_VERSION                        = "${params['asdb-version']}"
        ASDB_DISTRO                         = "${params['asdb-distro']}"
        ASDB_ARCHS                          = "${params['asdb-archs']}"
        ASDB_SIZE                           = "${params['asdb-size']}"
        ASDB_MACHINE_TYPE                   = "${params['asdb-machine-type']}"
        ASDB_DEVICE_TYPE                    = "${params['asdb-device-type']}"
        ASDB_NAMESPACES                     = "${params['asdb-namespaces']}"
        ABS_IMAGE_TAG                       = "${params['abs-image-tag']}"
        ABS_STORAGE_PROVIDER                = "${params['abs-storage-provider']}"
    }

    stages {
        stage("Look for existing GKE Cluster") {
            steps {
                script {
                    def downstream = build job: "devops/search-cloud-environment",
                        parameters: [string(name: "gke_name", value: "ecosys-gke")],
                        wait: true
                    env.GKE_DEPLOYED = downstream.description
                }
            }
        }

        stage("Deploy GKE Cluster") {
            when {
                expression {
                    return env.GKE_DEPLOYED == 'false'
                }
            }
            steps {
                build(job: 'devops/deploy-cloud-environment')
            }
        }

        stage("Deploy ASDB Cluster") {
            steps {
                build(job: 'devops/deploy-asdb', parameters: [
                    string(name: "ws", value: "${params['ws']}"),
                    string(name: "asdb-version", value: "${params['asdb-version']}"),
                    string(name: "asdb-distro", value: "${params['asdb-distro']}"),
                    string(name: "asdb-archs", value: "${params['asdb-archs']}"),
                    string(name: "asdb-size", value: "${params['asdb-size']}"),
                    string(name: "asdb-machine-type", value: "${params['asdb-machine-type']}"),
                    string(name: "asdb-device-type", value: "${params['asdb-device-type']}"),
                    string(name: "asdb-namespaces", value: "${params['asdb-namespaces']}")
                ], wait: true)
            }
        }

        stage("Deploy Aerospike Backup Service") {
            steps {
                build(job: 'devops/deploy-asdb', parameters: [
                    string(name: "ws", value: "${params['ws']}"),
                    string(name: "abs-image-tag", value: "${params['abs-image-tag']}"),
                    string(name: "abs-storage-provider", value: "${params['abs-storage-provider']}")
                ], wait: true)
            }
        }
    }

    post {
        success {
            build(job: 'devops/add-workspace', parameters: [
                string(name: "ws", value: "${params['ws']}"),
                string(name: "infra-branch", value: "$BRANCH_NAME"),
                string(name: "asdb-version", value: "${params['asdb-version']}"),
                string(name: "asdb-size", value: "${params['asdb-size']}"),
                string(name: "storage-provider", value: "${params['aerospike-backup-service-storage']}"),
                string(name: "backup-service-version", value: "${params['aerospike-backup-service-version']}")
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
                    [pattern: "**/.terragrunt-cache/", type: "INCLUDE"],
                    [pattern: "**/.terraform/", type: "INCLUDE"],
                    [pattern: "**/.terraform.lock.hcl", type: "INCLUDE"],
                    [pattern: "**/*.pem", type: "INCLUDE"],
                    [pattern: "**/*.jks", type: "INCLUDE"]
                ]
            )
        }
    }
}

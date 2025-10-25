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
            name: "helm-chart-source",
            choices: ["jfrog", "repo"],
            description: "Choose Helm Chart source"
        ),
        choiceParam(
            name: "abs-storage-provider",
            choices: ["gcp", "azure", "aws", "minio", "local"],
            description: "Choose storage provider"
        ),
        choiceParam(
            name: "asdb-device-type",
            choices: ["filesystem", "raw"],
            description: "Choose ASDB device type"
        ),
        stringParam(
            name: "abs-image-tag",
            defaultValue: "v3",
            description: "Set image tag"
        ),
        [
            $class: 'DynamicReferenceParameter',
            choiceType: 'ET_FORMATTED_HTML',
            name: 'abs-branch',
            description: 'Select a branch from aerospike-backup-service repo',
            referencedParameters: 'helm-chart-source',
            omitValueField: true,
            script: [
                $class: 'GroovyScript',
                fallbackScript: [
                    classpath: [],
                    sandbox: false,
                    script: 'return "<div>Error loading branches</div>"'
                ],
                script: [
                    classpath: [],
                    sandbox: false,
                    script: """
                        import groovy.json.JsonSlurper
                        import java.net.URL
                        import java.net.HttpURLConnection

                        def source = binding.variables.get("helm-chart-source")
                        if (!source?.trim()?.equalsIgnoreCase("repo")) {
                            return "<div>No abs branch selection needed</div>"
                        }

                        List<String> branches = []

                        try {
                            def githubToken = System.getenv("GITHUB_TOKEN")
                            def url = new URL("https://api.github.com/repos/aerospike/aerospike-backup-service/branches")
                            HttpURLConnection connection = (HttpURLConnection) url.openConnection()
                            connection.setRequestMethod("GET")
                            if (githubToken) {
                                connection.setRequestProperty("Authorization", "token \${githubToken}")
                            }

                            if (connection.responseCode == 200) {
                                def json = new JsonSlurper().parse(connection.inputStream)
                                branches = json.collect { it.name } as List<String>
                            } else {
                                return "<div>Error: \${connection.responseCode} \${connection.responseMessage}</div>"
                            }

                            connection.disconnect()
                        } catch (Exception e) {
                            return "<div>Exception: \${e.message}</div>"
                        }

                        def html = new StringBuilder()
                        html.append("<select name='value' class='setting-input' required>")
                        branches.each { branch ->
                            html.append("<option value='\${branch}'>\${branch}</option>")
                        }
                        html.append("</select>")

                        return html.toString()
                    """
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
        SERVICE_NAME         = 'aerospike-backup-service'
        CLOUDSDK_PYTHON_SITEPACKAGES = 1
        GITHUB_TOKEN         = credentials("github-token")
        DEBUG_MODE           = "${params['debug-mode']}"
        ABS_WORKSPACE        = "${params['ws']}"
        HELM_CHART_SOURCE    = "${params['helm-chart-source']}"
        ABS_BRANCH           = "${params['abs-branch']}"
        ABS_IMAGE_TAG = "${params["abs-image-tag"]}"
        ABS_STORAGE_PROVIDER = "${params["abs-storage-provider"]}"
        ASDB_DEVICE_TYPE = "${params["asdb-device-type"]}"
    }

    stages {
        stage("Generate common_vars.yaml") {
            environment {
                PROJECT_ID = "ecosystem-connectors-data"
                ABS_IMAGE_TAG = "${params["abs-image-tag"]}"
                ABS_STORAGE_PROVIDER = "${params["abs-storage-provider"]}"
                ASDB_DEVICE_TYPE = "${params["asdb-device-type"]}"
                ABS_WORKSPACE = "${params["ws"]}"
                HELM_CHART_SOURCE = "${params["helm-chart-source"]}"
            }
            steps {
                dir("./devops/backup-service/env/gcp/deploy-abs") {
                    sh 'envsubst < common_vars.tpl.yaml > common_vars.yaml'
                    sh "cat common_vars.yaml"
                }
            }
        }

        stage("Deploy Aerospike Backup Service") {
            environment {
                TF_INPUT = "0"
                TF_IN_AUTOMATION = "1"
                PATH = "/opt/python_venv/bin:${env.PATH}"
            }
            steps {
                dir("./devops/backup-service/env/gcp/deploy-abs") {
                    sh """
                    task apply
                    """
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
                    echo "SSH to the worker using the following command:"
                    echo "gcloud compute ssh --zone me-west1-a jenkins@${NODE_NAME} --tunnel-through-iap --project ecosystem-connectors-data"
                    input(
                        message: "Job is paused for debugging. Click 'Confirm proceed' to finish.",
                        parameters: [booleanParam(name: 'confirm-proceed', defaultValue: false, description: 'Confirm proceed')]
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

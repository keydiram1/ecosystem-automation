properties([
    parameters([
        booleanParam(name: "debug-mode", defaultValue: false, description: "Pauses job execution after the test run for debugging purposes"),
        [$class: 'ChoiceParameter',
            choiceType: 'PT_SINGLE_SELECT',
            description: 'Select the Workspace from the Dropdown List',
            filterLength: 1,
            filterable: false,
            name: 'ws',
            script: [
                $class: 'GroovyScript',
                fallbackScript: [
                    classpath: [],
                    sandbox: false,
                    script: "return ['Unable to get workspaces']"
                ],
                script: [
                    classpath: [],
                    sandbox: false,
                    script: '''
                        import groovy.sql.Sql
                        import java.sql.Driver
                        import java.util.Properties
                        import java.sql.Connection

                        List<String> workspaces = []
                        Properties props = new Properties()
                        String connectionUrl = "jdbc:sqlite:/data/jenkins.db"

                        Connection conn = null
                        try {
                            Driver driver = Class.forName('org.sqlite.JDBC').newInstance() as Driver
                            conn = driver.createConnection(connectionUrl, props)
                            Sql sql = new Sql(conn)

                            sql.eachRow("SELECT workspace FROM workspaces ORDER BY workspace ASC") { row ->
                                workspaces << row.workspace
                            }

                            sql.close()
                        } catch (Exception e) {
                            return ["Error fetching workspaces: ${e.message}"]
                        } finally {
                            if (conn != null) {
                                try {
                                    conn.close()
                                } catch (Exception ignored) {}
                            }
                        }

                        return workspaces
                    '''
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
        CLOUDSDK_PYTHON_SITEPACKAGES=1
        GITHUB_TOKEN = credentials("github-token")
        WS = "${params["ws"]}"
    }

    tools {
        ansible 'ansible-test'
    }

    stages {
        stage("Destroy GCP Env") {
            environment {
                PROJECT_ID = "ecosystem-connectors-data"
                BUCKET_NAME = "ecosys-workspace-vars"
            }
            steps {
                dir("./devops/env/gcp/stage") {
                   sh """
                    task destroy -- ${WS}
                    """
                }
            }
        }
    }
    post {
       success {
            build(job: 'devops/remove-workspace', parameters: [string(name: "ws", value: params["ws"])])
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
                    parameters: [booleanParam(name: 'confirm-proceed', defaultValue: false, description: 'Confirm proceed')])
                }
            }
            cleanWs(cleanWhenNotBuilt: false,
                    deleteDirs: true,
                    disableDeferredWipeout: true,
                    notFailBuild: true,
                    patterns: [[pattern: "**/.terragrunt-cache/", type: "INCLUDE"],
                               [pattern: "**/.terraform/", type: "INCLUDE"],
                               [pattern: "**/.terraform.lock.hcl", type: "INCLUDE"],
                               [pattern: "**/*.pem", type: "INCLUDE"],
                               [pattern: "**/*.jks", type: "INCLUDE"]])
       }
    }
}

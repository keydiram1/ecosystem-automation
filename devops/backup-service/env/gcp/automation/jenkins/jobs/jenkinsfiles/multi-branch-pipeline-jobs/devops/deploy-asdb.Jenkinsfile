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
        booleanParam(name: "asdb-load-balancer", defaultValue: false, description: "Enable Load Balancer for ASDB"),
        booleanParam(name: "multi-zone-deployment", defaultValue: false, description: "Deploy ASDB On Multiple Availability Zones"),
        [
            $class: 'DynamicReferenceParameter',
            choiceType: 'ET_FORMATTED_HTML',
            name: 'secret-agent',
            description: 'Enable Secret Agent if environment exists',
            referencedParameters: '',
            omitValueField: true,
            script: [
                $class: 'GroovyScript',
                fallbackScript: [
                    classpath: [],
                    sandbox: false,
                    script: 'return ["Error fetching environment"]'
                ],
                script: [
                    classpath: [],
                    sandbox: false,
                    script: """
                        import groovy.sql.Sql
                        import java.sql.Driver
                        import java.util.Properties
                        import java.sql.Connection

                        Properties props = new Properties()
                        String connectionUrl = "jdbc:sqlite:/data/jenkins.db"
                        String gkeName = "ecosys-gke"
                        String html = "<div>Secret Agent is not deployed</div>"
                        Connection conn = null

                        try {
                            Driver driver = Class.forName('org.sqlite.JDBC').newInstance() as Driver
                            conn = driver.connect(connectionUrl, props)
                            Sql sql = new Sql(conn)

                            def row = sql.firstRow("SELECT 1 FROM environments WHERE gke_name = ? LIMIT 1", [gkeName])
                            if (row != null) {
                                html = "<input type='checkbox' name='value' value='enabled' class='setting-input' checked>"
                            }

                            sql.close()
                        } catch (Exception e) {
                            html = "<div style='color:red;'>Error querying DB</div>"
                        } finally {
                            if (conn != null) {
                                try { conn.close() } catch (Exception ignored) {}
                            }
                        }
                        return html
                    """
                ]
            ]
        ],
        booleanParam(name: "asdb-device-shadow", defaultValue: false, description: "Enable Device Shadowing"),
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
                            conn = driver.connect(connectionUrl, props)
                            Sql sql = new Sql(conn)

                            sql.eachRow("SELECT version FROM asdb_download_center_versions") { row ->
                                versions << row.version
                            }

                            sql.close()
                        } catch (Exception e) {
                            return ["Error fetching versions: ${e.message}"]
                        } finally {
                            if (conn != null) {
                                try { conn.close() } catch (Exception ignored) {}
                            }
                        }

                        return [ "8.0.0.9" ] + versions.reverse().findAll { it != "8.0.0.9" }
                    '''
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
                    script: '''
                        import groovy.json.JsonSlurper
                        import groovy.sql.Sql
                        import java.sql.Driver
                        import java.sql.Connection
                        import java.util.Properties

                        List<String> distros = []
                        Properties props = new Properties()
                        String connectionUrl = "jdbc:sqlite:/data/jenkins.db"
                        Connection conn = null
                        Sql sql = null

                        try {
                            String selectedVersion = binding.variables.get("asdb-version")
                            Driver driver = Class.forName('org.sqlite.JDBC').newInstance() as Driver
                            conn = driver.connect(connectionUrl, props)
                            sql = new Sql(conn)

                            Map<String, Object> row = sql.firstRow("SELECT distros FROM asdb_download_center_versions WHERE version = ?", [selectedVersion])
                            if (row?.distros) {
                                distros = new JsonSlurper().parseText(row.distros as String) as List<String>
                            }

                            sql.close()
                        } catch (Exception e) {
                            return ["Error fetching distros: ${e.message}"]
                        } finally {
                            try { if (conn != null) conn.close() } catch (Exception ignored) {}
                        }

                        return distros
                    '''
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
                    script: '''
                        import groovy.json.JsonSlurper
                        import groovy.sql.Sql
                        import java.sql.Driver
                        import java.sql.Connection
                        import java.util.Properties

                        List<String> archs = []
                        Properties props = new Properties()
                        String connectionUrl = "jdbc:sqlite:/data/jenkins.db"
                        Connection conn = null
                        Sql sql = null

                        try {
                            String selectedVersion = binding.variables.get("asdb-version")
                            Driver driver = Class.forName('org.sqlite.JDBC').newInstance() as Driver
                            conn = driver.connect(connectionUrl, props)
                            sql = new Sql(conn)

                            Map<String, Object> row = sql.firstRow("SELECT archs FROM asdb_download_center_versions WHERE version = ?", [selectedVersion])
                            if (row?.archs) {
                                archs = new JsonSlurper().parseText(row.archs as String) as List<String>
                                archs.sort()
                            }

                            sql.close()
                        } catch (Exception e) {
                            return ["Error fetching archs: ${e.message}"]
                        } finally {
                            try { conn?.close() } catch (Exception ignored) {}
                        }

                        return archs
                    '''
                ]
            ]
        ],
        [
            $class: 'CascadeChoiceParameter',
            choiceType: 'PT_SINGLE_SELECT',
            description: 'Select ASDB Cluster Size',
            referencedParameters: 'asdb-device-shadow',
            filterLength: 1,
            filterable: false,
            name: 'asdb-size',
            script: [
                $class: 'GroovyScript',
                fallbackScript: [
                    classpath: [],
                    sandbox: false,
                    script: 'return ["3"]'
                ],
                script: [
                    classpath: [],
                    sandbox: false,
                    script: '''
                        try {
                            String selectedDeviceShadow = binding.variables.get("asdb-device-shadow")
                            if (selectedDeviceShadow) {
                                return ["5"]
                            }
                            return ["3", "1", "5", "7"]
                        } catch (Exception e) {
                            return ["Error fetching cluster size: ${e.message}"]
                        }
                    '''
                ]
            ]
        ],
        [
            $class: 'CascadeChoiceParameter',
            choiceType: 'PT_SINGLE_SELECT',
            description: 'Select ASDB Machine Type"',
            referencedParameters: 'asdb-device-shadow',
            filterLength: 1,
            filterable: false,
            name: 'asdb-machine-type',
            script: [
                $class: 'GroovyScript',
                fallbackScript: [
                    classpath: [],
                    sandbox: false,
                    script: 'return ["n2-standard-16"]'
                ],
                script: [
                    classpath: [],
                    sandbox: false,
                    script: '''
                        try {
                            String selectedDeviceShadow = binding.variables.get("asdb-device-shadow")
                            if (selectedDeviceShadow) {
                                return ["n2-standard-16"]
                            }
                            return ["n2-standard-8", "n2-standard-4", "n2-standard-16"]
                        } catch (Exception e) {
                            return ["Error fetching machine type: ${e.message}"]
                        }
                    '''
                ]
            ]
        ],
        choiceParam(name: "asdb-security-type", choices: ["TLS", "clear-text", "mTLS"], description: "Select ASDB Encryption Type"),
        [
            $class: 'DynamicReferenceParameter',
            choiceType: 'ET_FORMATTED_HTML',
            name: 'asdb-section-selection',
            description: 'Select ASDB sections to enable',
            referencedParameters: 'asdb-security-type',
            omitValueField: true,
            script: [
                $class: 'GroovyScript',
                fallbackScript: [
                    classpath: [],
                    sandbox: false,
                    script: 'return ["Error generating section selection"]'
                ],
                script: [
                    classpath: [],
                    sandbox: false,
                    script: """
                        String securityType = binding.variables.get("asdb-security-type")

                        if (securityType == "clear-text") {
                            return "<div>No section selection needed</div>"
                        } else {
                            return '''
                                <div>
                                    <label><input type="checkbox" name="value" value="service" checked> Service</label><br>
                                    <label><input type="checkbox" name="value" value="heartbeat"> Heartbeat</label><br>
                                    <label><input type="checkbox" name="value" value="fabric"> Fabric</label>
                                </div>
                            '''
                        }
                    """
                ]
            ]
        ],
        [
            $class: 'CascadeChoiceParameter',
            choiceType: 'PT_SINGLE_SELECT',
            description: 'Select ASDB Local SSD Device Type',
            referencedParameters: 'asdb-device-shadow',
            filterLength: 1,
            filterable: false,
            name: 'asdb-device-type',
            script: [
                $class: 'GroovyScript',
                fallbackScript: [
                    classpath: [],
                    sandbox: false,
                    script: 'return ["filesystem"]'
                ],
                script: [
                    classpath: [],
                    sandbox: false,
                    script: '''
                        try {
                            String selectedDeviceShadow = binding.variables.get("asdb-device-shadow")
                            if (selectedDeviceShadow) {
                                return ["raw"]
                            }
                            return ["filesystem", "raw"]
                        } catch (Exception e) {
                            return ["Error fetching device types: ${e.message}"]
                        }
                    '''
                ]
            ]
        ],
        [
            $class: 'DynamicReferenceParameter',
            choiceType: 'ET_FORMATTED_HTML',
            name: 'asdb-devices',
            description: 'Select number of attached SSD devices',
            referencedParameters: 'asdb-device-type,asdb-machine-type,asdb-device-shadow',
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
                        String selectedDeviceShadow = binding.variables.get("asdb-device-shadow")
                        if (selectedDeviceShadow) {
                            return "<select name=\\"value\\" class=\\"setting-input\\" required>" +
                                   "<option value=\\"2\\">2</option>" +
                                   "</select>"
                        }
                        String deviceType = binding.variables.get("asdb-device-type")
                        String machineType = binding.variables.get("asdb-machine-type")

                        if (deviceType != null && deviceType == "raw") {
                            if (machineType != null && machineType.contains("-")) {
                                int machineSize = machineType.tokenize("-")[-1].toInteger()
                                if (machineSize < 16) {
                                    return "<select name=\\"value\\" class=\\"setting-input\\" required>" +
                                           "<option value=\\"1\\">1</option>" +
                                           "<option value=\\"2\\">2</option>" +
                                           "<option value=\\"4\\">4</option>" +
                                           "<option value=\\"8\\">8</option>" +
                                           "<option value=\\"16\\">16</option>" +
                                           "<option value=\\"24\\">24</option>" +
                                           "</select>"
                                } else {
                                    return "<select name=\\"value\\" class=\\"setting-input\\" required>" +
                                           "<option value=\\"2\\">2</option>" +
                                           "<option value=\\"4\\">4</option>" +
                                           "<option value=\\"8\\">8</option>" +
                                           "<option value=\\"16\\">16</option>" +
                                           "<option value=\\"24\\">24</option>" +
                                           "</select>"
                                }
                            } else {
                                return "<div>Invalid machine type format</div>"
                            }
                        } else {
                            return "<div>No device selection needed</div>"
                        }
                    """
                ]
            ]
        ],
        [
            $class: 'CascadeChoiceParameter',
            choiceType: 'PT_SINGLE_SELECT',
            description: 'Select ASDB namespaces',
            referencedParameters: 'asdb-device-type,asdb-devices,asdb-device-shadow',
            filterLength: 1,
            filterable: false,
            name: 'asdb-namespaces',
            script: [
                $class: 'GroovyScript',
                fallbackScript: [
                    classpath: [],
                    sandbox: false,
                    script: 'return ["Error"]'
                ],
                script: [
                    classpath: [],
                    sandbox: false,
                    script: """
                        String selectedDeviceShadow = binding.variables.get("asdb-device-shadow")
                        if (selectedDeviceShadow) {
                            return ["1"]
                        }
                        String selectedType = binding.variables.get("asdb-device-type")
                        List<String> namespaces = []
                        if (selectedType == "raw") {
                            int selectedDevice = binding.variables.get("asdb-devices").toInteger()
                            if (selectedDevice == 1) {
                                namespaces = ["1"]
                            } else if (selectedDevice == 2) {
                                namespaces = ["1", "2"]
                            } else {
                                String halfValue = ((int)(selectedDevice / 2)).toString()
                                namespaces = ["1", "2", halfValue, selectedDevice.toString()]
                            }
                        } else {
                            namespaces = (1..30).collect { it.toString() }.reverse()
                        }
                        return namespaces
                    """
                ]
            ]
        ],
        [
            $class: 'DynamicReferenceParameter',
            choiceType: 'ET_FORMATTED_HTML',
            description: 'Select Single Query Threads',
            referencedParameters: '',
            omitValueField: true,
            name: 'asdb-single-query-threads',
            script: [
                $class: 'GroovyScript',
                fallbackScript: [
                    classpath: [],
                    sandbox: false,
                    script: 'return "<div>Error Setting Single Query Threads Parameter</div>"'
                ],
                script: [
                    classpath: [],
                    sandbox: false,
                    script: """
                         return "<input type=\\"number\\" name=\\"value\\" class=\\"setting-input\\" step=\\"1\\" min=\\"4\\" max=\\"24\\" value=\\"12\\">"
                    """
                ]
            ]
        ],
        booleanParam(name: "encryption-at-rest-enabled", defaultValue: false, description: "Select Encryption At Rest"),
        booleanParam(name: "asdb-sc-enabled", defaultValue: false, description: "Select Strong Consistency"),
        [
            $class: 'DynamicReferenceParameter',
            choiceType: 'ET_FORMATTED_HTML',
            description: 'Select Rooster',
            referencedParameters: 'asdb-sc-enabled',
            omitValueField: true,
            name: 'asdb-roster',
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
                    script: '''
                        Boolean asdbScEnabled = binding.variables.get("asdb-sc-enabled")
                        if (asdbScEnabled) {
                            return "<input type='checkbox' name='value' value='enabled' class='setting-input'>"
                        }
                        return "<div>No Rooster selection needed</div>"
                    '''
                ]
            ]
        ],
        booleanParam(name: "ops-agent", defaultValue: false, description: "Select Ops Agent")
    ])
])

def setDefaultEnv(varName, defaultValue) {
    if (!env."${varName}") {
        env."${varName}" = defaultValue
    }
}

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
                ASDB_WORKSPACE          = "${params['ws']}"
                SECRET_AGENT       = "${params['secret-agent']}"
                ASDB_VERSION       = "${params['asdb-version']}"
                ASDB_DISTRO        = "${params['asdb-distro']}"
                ASDB_ARCHS         = "${params['asdb-archs']}"
                ASDB_SIZE          = "${params['asdb-size']}"
                ASDB_MACHINE_TYPE  = "${params['asdb-machine-type']}"
                ASDB_SECURITY_TYPE    = "${params['asdb-security-type']}"
                ASDB_SECTION_SELECTION    = "${params['asdb-section-selection']}"
                ASDB_DEVICE_TYPE   = "${params['asdb-device-type']}"
                ASDB_DEVICES            = "${params['asdb-devices']}"
                ASDB_NAMESPACES    = "${params['asdb-namespaces']}"
                ASDB_SC_ENABLED    = "${params['asdb-sc-enabled']}"
                ASDB_ROSTER        = "${params['asdb-roster']}"
                ASDB_DEVICE_SHADOW    = "${params['asdb-device-shadow']}"
                ASDB_LOAD_BALANCER    = "${params['asdb-load-balancer']}"
                OPS_AGENT          = "${params['ops-agent']}"
                ENCRYPTION_AT_REST_ENABLED = "${params['encryption-at-rest-enabled']}"
                MULTI_ZONE_DEPLOYMENT = "${params['multi-zone-deployment']}"
                ASDB_SINGLE_QUERY_THREADS =  "${params['asdb-single-query-threads']}"

            }
            steps {
                dir("./devops/backup-service/env/gcp/deploy-asdb") {
                    script {
                        setDefaultEnv('SECRET_AGENT', 'false')
                        setDefaultEnv('ASDB_ROSTER', 'false')
                        setDefaultEnv('ASDB_DEVICES', '1')
                    }
                    sh '''
                        echo "rooster is: $ASDB_ROSTER"
                        export SECRET_AGENT="${SECRET_AGENT:-false}"
                        export ASDB_ROSTER="${ASDB_ROSTER:-false}"
                        export ASDB_DEVICES="${ASDB_DEVICES:-1}"
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
                dir("./devops/backup-service/env/gcp/deploy-asdb") {
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


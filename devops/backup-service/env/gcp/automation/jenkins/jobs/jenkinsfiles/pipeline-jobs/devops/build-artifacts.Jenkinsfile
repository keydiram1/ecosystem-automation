@Library('jenkins-devops-lib') _

properties([
    parameters([
        booleanParam(
            name: "debug-mode",
            defaultValue: false,
            description: "Pauses job execution after the test run for debugging purposes"
        ),
        [
            $class: 'DynamicReferenceParameter',
            choiceType: 'ET_FORMATTED_HTML',
            name: 'repository',
            description: 'Select GitHub Repository',
            referencedParameters: '',
            omitValueField: true,
            script: [
                $class: 'GroovyScript',
                fallbackScript: [
                    classpath: [],
                    sandbox: false,
                    script: 'return "<div>Error loading repository choices</div>"'
                ],
                script: [
                    classpath: [],
                    sandbox: false,
                    script: '''
                        Map<String, String> repoMap = [
                            "aerospike-backup-service": "https://github.com/aerospike/aerospike-backup-service.git",
                            "aerospike-backup-cli" : "https://github.com/aerospike/aerospike-backup-cli.git"
                        ]
                        StringBuilder html = new StringBuilder("<select name='value' class='setting-input' required>")
                        repoMap.each { label, value ->
                            html.append("<option value='${value}'>${label}</option>")
                        }
                        html.append("</select>")
                        return html.toString()
                    '''
                ]
            ]
        ],
        [
            $class: 'DynamicReferenceParameter',
            choiceType: 'ET_FORMATTED_HTML',
            name: 'branch',
            description: 'Select a branch from the selected GitHub repository',
            referencedParameters: 'repository',
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
                    script: '''
                        import groovy.json.JsonSlurper
                        import java.net.URL
                        import java.net.HttpURLConnection

                        String selectedRepository = binding.variables.get("repository")
                        List<String> priorityBranches = ["v3", "main"]
                        List<String> branches = []

                        if (!selectedRepository) {
                            return "<div>Error: No repository specified</div>"
                        }

                        HttpURLConnection connection = null
                        try {
                            String repoName = selectedRepository.tokenize('/')[-1].replace('.git', '')
                            String apiUrl = "https://api.github.com/repos/aerospike/${repoName}/branches"
                            URL url = new URL(apiUrl)
                            connection = (HttpURLConnection) url.openConnection()
                            connection.setRequestMethod("GET")
                            connection.setRequestProperty("User-Agent", "Groovy-Script")
                            connection.setConnectTimeout(5000)
                            connection.setReadTimeout(5000)

                            String githubToken = System.getenv("GITHUB_TOKEN")
                            if (githubToken) {
                                connection.setRequestProperty("Authorization", "token ${githubToken}")
                            }

                            if (connection.responseCode == 200) {
                                def jsonResponse = new JsonSlurper().parse(connection.inputStream)
                                branches = jsonResponse.collect { it.name }
                            } else {
                                return "<div>Error: ${apiUrl} ${connection.responseCode} ${connection.responseMessage}</div>"
                            }
                        } catch (Exception e) {
                            return "<div>Exception: ${e.message?.replaceAll('<', '&lt;')?.replaceAll('>', '&gt;')}</div>"
                        } finally {
                            connection?.disconnect()
                        }

                        List<String> sortedBranches = []
                        priorityBranches.each {
                            if (branches.contains(it)) sortedBranches << it
                        }
                        sortedBranches.addAll(branches.findAll { !priorityBranches.contains(it) })

                        def html = new StringBuilder("<select name='value' class='setting-input' required>")
                        sortedBranches.each {
                            html.append("<option value='${it}'>${it}</option>")
                        }
                        html.append("</select>")
                        return html.toString()
                    '''
                ]
            ]
        ],
        [
            $class: 'DynamicReferenceParameter',
            choiceType: 'ET_FORMATTED_HTML',
            name: 'artifacts',
            description: 'Select Artifacts',
            referencedParameters: '',
            omitValueField: true,
            script: [
                $class: 'GroovyScript',
                fallbackScript: [
                    classpath: [],
                    sandbox: false,
                    script: 'return "<div>Error loading options</div>"'
                ],
                script: [
                    classpath: [],
                    sandbox: false,
                    script: '''
                        List<String> allOptions = ["docker", "helm", "deb", "rpm"]
                        List<String> defaultSelected = ["docker"]
                        StringBuilder html = new StringBuilder()
                        allOptions.each { opt ->
                            boolean checked = defaultSelected.contains(opt)
                            html.append("<label style='display:block'>")
                            html.append("<input type='checkbox' name='value' value='${opt}' ${checked ? 'checked' : ''}/> ${opt}")
                            html.append("</label>")
                        }
                        return html.toString()
                    '''
                ]
            ]
        ],
        [
            $class: 'DynamicReferenceParameter',
            choiceType: 'ET_FORMATTED_HTML',
            name: 'tag',
            description: 'Enter a version tag',
            referencedParameters: 'branch,artifacts',
            omitValueField: true,
            script: [
                $class: 'GroovyScript',
                fallbackScript: [
                    classpath: [],
                    sandbox: false,
                    script: 'return "<input type=\'text\' name=\'value\' value=\'v3\' class=\'setting-input\' required/>"'
                ],
                script: [
                    classpath: [],
                    sandbox: false,
                    script: '''
                        String selectedArtifacts = binding.variables.get("artifacts") as String
                        if (selectedArtifacts.contains("docker") || selectedArtifacts.contains("rpm") || selectedArtifacts.contains("deb")) {
                            String selectedBranch = binding.variables.get("branch") as String
                            return "<input type='text' name='value' value='${selectedBranch}' class='setting-input' required/>"
                        }
                        return "<div>No abs tag naming needed</div>"
                    '''
                ]
            ]
        ],
        [
            $class: 'DynamicReferenceParameter',
            choiceType: 'ET_FORMATTED_HTML',
            name: 'helm-version',
            description: 'Enter a helm version',
            referencedParameters: 'artifacts',
            omitValueField: true,
            script: [
                $class: 'GroovyScript',
                fallbackScript: [
                    classpath: [],
                    sandbox: false,
                    script: 'return "<input type=\'text\' name=\'value\' value=\'0.0.1\' class=\'setting-input\' required/>"'
                ],
                script: [
                    classpath: [],
                    sandbox: false,
                    script: '''
                        String selectedArtifacts = binding.variables.get("artifacts") as String
                        if (selectedArtifacts.contains("helm")) {
                            return "<input type='text' name='value' value='0.0.1' class='setting-input' required/>"
                        }
                        return "<div>No helm version setting needed</div>"
                    '''
                ]
            ]
        ],
        [
            $class: 'DynamicReferenceParameter',
            choiceType: 'ET_FORMATTED_HTML',
            name: 'architecture',
            description: 'Select architecture targets (default: both selected)',
            referencedParameters: 'artifacts',
            omitValueField: true,
            script: [
                $class: 'GroovyScript',
                fallbackScript: [
                    classpath: [],
                    sandbox: false,
                    script: 'return "<div>Error loading architectures</div>"'
                ],
                script: [
                    classpath: [],
                    sandbox: false,
                    script: '''
                        String selectedArtifacts = binding.variables.get("artifacts") as String
                        if (selectedArtifacts.contains("docker") || selectedArtifacts.contains("rpm") || selectedArtifacts.contains("deb")) {
                            Map<String, String> archMap = ["amd64": "linux/amd64", "arm64": "linux/arm64"]
                            List<String> defaultSelected = ["linux/amd64", "linux/arm64"]
                            StringBuilder html = new StringBuilder()
                            archMap.each { label, value ->
                                boolean checked = defaultSelected.contains(value)
                                html.append("<label style='display:block'>")
                                html.append("<input type='checkbox' name='value' value='${value}' ${checked ? 'checked' : ''}/> ${label}")
                                html.append("</label>")
                            }
                            return html.toString()
                        }
                        return "<div>No Architecture setting needed</div>"
                    '''
                ]
            ]
        ],
        choice(
            name: "jfrog-repo",
            description: "Choose JFrog repository",
            choices: ["dev", "preview"]
        )
    ])
])


pipeline {
    agent {
        label "jenkins-artifact-builder"
    }

    tools {
        go 'Default'
    }

    options {
        timeout(time: 1, unit: "HOURS")
        buildDiscarder(logRotator(numToKeepStr: "20", daysToKeepStr: "30"))
        overrideIndexTriggers(false)
    }

    environment {
        DEBUG_MODE     = "${params['debug-mode']}"
        REPOSITORY     = "${params.repository}"
        JFROG_REPO     = "${params['jfrog-repo']}"
        BRANCH         = "${params.branch}"
        IMAGE_TAG      = "${params.tag}"
        ARCHITECTURE   = "${params.architecture}"
        HELM_VERSION   = "${params['helm-version']}"
        REGISTRY       = "aerospike.jfrog.io/ecosystem-dockerhub-mirror"
        GOMODCACHE     = "/home/jenkins/.gomodcache"
    }

    stages {
        stage("Checkout") {
            steps {
                checkout([
                  $class: 'GitSCM',
                  branches: [[name: "*/${env.BRANCH}"]],
                  userRemoteConfigs: [[url: env.REPOSITORY]],
                  doGenerateSubmoduleConfigurations: false,
                  extensions: [
                    [$class: 'LocalBranch', localBranch: env.BRANCH],
                    [$class: 'CloneOption', noTags: true, shallow: true, depth: 1],
                    [$class: 'SubmoduleOption',
                      disableSubmodules: false,
                      parentCredentials: true,
                      recursiveSubmodules: true,
                      trackingSubmodules: false
                    ]
                  ]
                ])
            }
        }

        stage("Snyk Repository Scan") {
            steps {
                script {
                    try {
                        withCredentials([
                            string(credentialsId: "snyk-token", variable: "SNYK_TOKEN")
                        ]) {
                            sh '''
                                echo "Verifying Go modules..."

                                if [ ! -f "go.mod" ]; then
                                    echo "Error: go.mod file not found. Are you in a Go module directory?"
                                    exit 1
                                fi

                                if ! go mod verify; then
                                    echo "Go module verification failed. Running go mod tidy..."
                                    go mod tidy
                                    echo "Re-verifying modules after tidy..."
                                    if ! go mod verify; then
                                        echo "Module verification still failing after tidy. Manual intervention required."
                                        exit 1
                                    fi
                                    echo "Modules verified successfully after tidy!"
                                else
                                    echo "Go modules verified successfully!"
                                fi

                                make vulnerability-scan
                            '''
                        }
                        echo "Snyk scan completed successfully"
                    } catch (Exception e) {
                        echo "Snyk scan failed with error: ${e.getMessage()}"
                        currentBuild.result = 'UNSTABLE'
                    }
                }
            }
        }

        stage('Build Artifacts') {
            steps {
                script {
                    def architectureParam = params.architecture
                    def selectionsArchs = architectureParam.tokenize(',').collect { it.trim().toLowerCase() }
                    def archs = []

                    if (selectionsArchs.size() >= 1 && selectionsArchs[0] == "true") {
                        archs << "linux/amd64"
                    }
                    if (selectionsArchs.size() >= 2 && selectionsArchs[1] == "true") {
                        archs << "linux/arm64"
                    }

                    env.ARCHS = archs.join(' ')
                    def artifactList = params.artifacts.split(',').collect { it.toBoolean() }

                    if (artifactList[0]) {
                        echo "Build Docker Images"

                        if (env.JFROG_REPO == "dev") {
                            env.IMAGE_REPO = "aerospike.jfrog.io/ecosystem-container-dev-local"
                        } else if (env.JFROG_REPO == "preview") {
                            env.IMAGE_REPO = "aerospike.jfrog.io/ecosystem-container-preview-public-local"
                        } else {
                            error "Unknown JFROG_REPO value: ${env.JFROG_REPO}. Expected 'dev' or 'preview'"
                        }

                        withCredentials([
                            string(credentialsId: "docker-username", variable: "DOCKER_USERNAME"),
                            string(credentialsId: "docker-password", variable: "DOCKER_PASSWORD"),
                            string(credentialsId: "jfrog-username", variable: "JFROG_USERNAME"),
                            string(credentialsId: "jfrog-token", variable: "JFROG_TOKEN")
                        ]) {
                            sh '''
                                set -e
                                SERVICE_NAME="${repository##*/}"
                                SERVICE_NAME="${SERVICE_NAME%.git}"
                                export GOPROXY="https://$JFROG_USERNAME:$JFROG_TOKEN@aerospike.jfrog.io/artifactory/api/go/ecosystem-go-dependency-cache-virtual"

                                docker buildx create --name builder --driver docker-container --use
                                docker buildx inspect --bootstrap
                                IMAGE_CACHE_FROM=type=registry,ref=aerospike.jfrog.io/ecosystem-container-dev-local/$SERVICE_NAME:cache \
                                IMAGE_CACHE_TO=type=registry,ref=aerospike.jfrog.io/ecosystem-container-dev-local/$SERVICE_NAME:cache,mode=max \
                                IMAGE_REPO="$IMAGE_REPO" \
                                IMAGE_TAG=$IMAGE_TAG \
                                ARCHS=$ARCHS \
                                make docker-buildx
                                unset GOPROXY
                            '''
                        }

                        try {
                            withCredentials([
                                string(credentialsId: "snyk-token", variable: "SNYK_TOKEN"),
                                string(credentialsId: "docker-username", variable: "SNYK_REGISTRY_USERNAME"),
                                string(credentialsId: "docker-password", variable: "SNYK_REGISTRY_PASSWORD")
                            ]) {
                                sh '''
                                    SERVICE_NAME="${repository##*/}"
                                    SERVICE_NAME="${SERVICE_NAME%.git}"
                                    IMAGE_REPO=$IMAGE_REPO/$SERVICE_NAME IMAGE_TAG=$IMAGE_TAG make vulnerability-scan-container
                                '''
                            }
                            echo "Snyk container scan completed successfully"
                        } catch (Exception e) {
                            echo "Snyk container scan failed with error: ${e.getMessage()}"
                            currentBuild.result = 'UNSTABLE'
                        }
                    }

                    if (artifactList[1]) {
                        echo "Build Helm Charts"
                        withCredentials([
                            string(credentialsId: "jfrog-username", variable: "JFROG_USERNAME"),
                            string(credentialsId: "jfrog-token", variable: "JFROG_TOKEN")
                        ]) {
                            sh '''
                                if [ "$JFROG_REPO" = "dev" ]; then
                                    HELM_REPO="ecosystem-helm-dev-local"
                                elif [ "$JFROG_REPO" = "preview" ]; then
                                    HELM_REPO="ecosystem-helm-preview-public-local"
                                else
                                    echo "Error: Unknown JFROG_REPO value: $JFROG_REPO"
                                    echo "Expected values: 'dev' or 'preview'"
                                    exit 1
                                fi

                                jf config add "aerospike" \
                                    --user="$JFROG_USERNAME" \
                                    --password="$JFROG_TOKEN" \
                                    --url="https://aerospike.jfrog.io" \
                                    --artifactory-url="https://aerospike.jfrog.io/artifactory" \
                                    --distribution-url="https://aerospike.jfrog.io/distribution" \
                                    --xray-url=https://aerospike.jfrog.io/xray \
                                    --interactive=false \
                                    --overwrite=true

                                REPO_NAME="${repository##*/}"
                                REPO_NAME="${REPO_NAME%.git}"

                                helm package "./helm/$REPO_NAME" \
                                    --version "$HELM_VERSION" \
                                    --destination "/tmp/workspace/devops/$branch"

                                jf rt upload \
                                    "/tmp/workspace/devops/$branch/$REPO_NAME-$HELM_VERSION.tgz" \
                                    "$HELM_REPO/$REPO_NAME/$HELM_VERSION/$REPO_NAME-$HELM_VERSION.tgz" \
                                    --build-name="$REPO_NAME-helm" --build-number="$HELM_VERSION"

                                rm "/tmp/workspace/devops/$branch/$REPO_NAME-$HELM_VERSION.tgz"
                            '''
                        }
                    }

                    if (artifactList[2] || artifactList[3]) {
                        echo "Build Linux Packages"

                        def packagersParam = params.artifacts
                        def selectionsPacks = packagersParam.tokenize(',').collect { it.trim().toLowerCase() }
                        def packagers = []

                        if (selectionsPacks.size() >= 3 && selectionsPacks[2] == "true") {
                            packagers << "deb"
                        }
                        if (selectionsPacks.size() >= 4 && selectionsPacks[3] == "true") {
                            packagers << "rpm"
                        }

                        env.PACKAGERS = packagers.join(' ')

                        withCredentials([
                            string(credentialsId: "jfrog-username", variable: "JFROG_USERNAME"),
                            string(credentialsId: "jfrog-token", variable: "JFROG_TOKEN")
                        ]) {
                            sh '''
                                set -e

                                if [ "$JFROG_REPO" = "dev" ]; then
                                    RPM_REPO="ecosystem-rpm-dev-local"
                                    DEB_REPO="ecosystem-deb-dev-local"
                                elif [ "$JFROG_REPO" = "preview" ]; then
                                    RPM_REPO="ecosystem-rpm-preview-public-local"
                                    DEB_REPO="ecosystem-deb-preview-public-local"
                                else
                                    echo "Error: Unknown JFROG_REPO value: $JFROG_REPO"
                                    echo "Expected values: 'dev' or 'preview'"
                                    exit 1
                                fi

                                export GOPROXY="https://$JFROG_USERNAME:$JFROG_TOKEN@aerospike.jfrog.io/artifactory/api/go/ecosystem-go-dependency-cache-virtual"
                                VERSION=$IMAGE_TAG ARCHS=$ARCHS PACKAGERS=$PACKAGERS make packages
                                unset GOPROXY

                                jf config add "aerospike" \
                                    --user="$JFROG_USERNAME" \
                                    --password="$JFROG_TOKEN" \
                                    --url="https://aerospike.jfrog.io" \
                                    --artifactory-url="https://aerospike.jfrog.io/artifactory" \
                                    --distribution-url="https://aerospike.jfrog.io/distribution" \
                                    --xray-url=https://aerospike.jfrog.io/xray \
                                    --interactive=false \
                                    --overwrite=true

                                REPO_NAME="${repository##*/}"
                                REPO_NAME="${REPO_NAME%.git}"

                                for file in $(find ./build -type f -name "*.rpm"); do
                                    arch="${file%.rpm}"
                                    arch="${arch##*.}"
                                    echo "Processing RPM: $file"
                                    echo "Arch: $arch"

                                    jf rt upload "$file" \
                                        "$RPM_REPO/$REPO_NAME/$branch/$(basename $file)" \
                                        --build-name="$REPO_NAME-rpm" \
                                        --build-number="$branch" \
                                        --target-props="rpm.distribution=stable;rpm.component=main;rpm.architecture=$arch"
                                done

                                for file in $(find ./build -type f -name "*.deb"); do
                                    arch="${file%.deb}"
                                    arch="${arch##*_}"
                                    echo "Processing DEB: $file"
                                    echo "Arch: $arch"

                                    jf rt upload "$file" \
                                        "$DEB_REPO/$REPO_NAME/$branch/$(basename $file)" \
                                        --build-name="$REPO_NAME-deb" \
                                        --build-number="$branch" \
                                        --target-props="deb.distribution=stable;deb.component=main;deb.architecture=$arch"
                                done
                            '''
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            sh '''
                make clean
                docker context use default
                if docker buildx ls | grep -q builder; then
                    docker buildx rm builder
                fi
                '''
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
                        parameters: [
                            booleanParam(
                                name: 'confirm-proceed',
                                defaultValue: false,
                                description: 'Confirm proceed'
                            )
                        ]
                    )
                }
            }

            cleanWs(
                cleanWhenNotBuilt: false,
                deleteDirs: true,
                disableDeferredWipeout: true,
                notFailBuild: true
            )
        }
    }
}

import groovy.json.JsonSlurper

def getRPToken() {
    def jsonSlurper = new JsonSlurper()
    def jsonData = jsonSlurper.parseText("${RP_TOKEN}")
    def attributeValue = jsonData['access_token']
    return attributeValue
}

def uninstall() {
    sh 'docker rm -vf $(docker ps -aq) || true'
    sh 'docker volume prune -f'
    sh 'docker system prune -f'
}

def dockerLoginToJfrog() {
    withCredentials([
            string(credentialsId: "docker-username", variable: "DOCKER_USERNAME"),
            string(credentialsId: "docker-password", variable: "DOCKER_PASSWORD")
    ]) {
        sh 'docker login "$DOCKER_REGISTRY" -u "$DOCKER_USERNAME" -p "$DOCKER_PASSWORD"'
    }
}

def cleanAll() {
    cleanWs(cleanWhenNotBuilt: true,
            deleteDirs: true,
            disableDeferredWipeout: true,
            notFailBuild: true)
    uninstall()
}

def installMultiNodeENV() {
    dir("./devops/env/local") {
        ansiblePlaybook(
                installation: 'ansible-test',
                playbook: 'install-infra.yaml',
                inventory: 'localhost',
                extras: "--extra-vars '{\"cli\": {\"version\": \"${ASDB_VERSION}\", \"size\": ${ASDB_SIZE}}}'",
                colorized: true)
    }
}

def fetchNetworkDetails() {
    sh '''
            set +e
            while true; do
              if  kubectl --namespace aerospike get pods --selector=statefulset.kubernetes.io/pod-name &> /dev/null; then
                kubectl --namespace aerospike wait pods \
                --selector=statefulset.kubernetes.io/pod-name --for=condition=ready --timeout=180s
                break
              fi
            done
            set -e
        '''
    script {
        env.SECRET_AGENT_IP = sh(
                script: "kubectl get nodes -o jsonpath='" +
                        "{range .items[*]}" +
                        "{.status.addresses[?(@.type==\"ExternalIP\")].address}" +
                        "{.status.addresses[?(@.type==\"InternalIP\")].address}{end}'",
                returnStdout: true
        ).trim()

        env.SECRET_AGENT_PORT = sh(
                script: "kubectl get service " +
                        "--namespace aerospike aerospike-secret-agent-external " +
                        "-o jsonpath='{.spec.ports[?(@.name==\"secret-agent\")].nodePort}'",
                returnStdout: true
        ).trim()

        env.ASDB_IP = sh(
                script: "kubectl get nodes -o jsonpath='" +
                        "{range .items[*]}" +
                        "{.status.addresses[?(@.type==\"ExternalIP\")].address}" +
                        "{.status.addresses[?(@.type==\"InternalIP\")].address}{end}'",
                returnStdout: true
        ).trim()

        env.ASDB_PORT = sh(
                script: "kubectl get service " +
                        "--namespace aerospike aerocluster-0-0 " +
                        "-o jsonpath='{.spec.ports[0].nodePort}'",
                returnStdout: true
        ).trim()

        env.LOCAL_SLAVE_GATEWAY = sh(
                script: "docker inspect " +
                        "--format '{{ .NetworkSettings.Networks.kind.Gateway }}' " +
                        "abs-cluster-control-plane",
                returnStdout: true
        ).trim()
}}

def removeMultiNodeENV() {
    dir("./devops/env/local") {
        ansiblePlaybook(
                installation: 'ansible-test',
                playbook: 'uninstall-infra.yaml',
                inventory: 'localhost',
                extras: "--extra-vars '{\"cli\": {\"version\": \"${ASDB_VERSION}\", \"size\": ${ASDB_SIZE}}}'",
                colorized: true)
    }

    cleanWs(deleteDirs: true)
}

return this
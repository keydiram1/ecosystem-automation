def parallel_classes
def asdb_version
def jvm_suspend
def spring_data_branch
def notes
def spring_data_version

def install() {
    setDefaultValuesForChoiceParameters()
    cloneAndCheckoutSpringData()
    dir("./devops/install/spring-data") {
        sh 'echo  "\nASDB_VERSION=${asdb_version}" >> .env'
        sh 'echo  "\nPATH_TO_SPRING_DATA_PROJECT=/opt/automation/spring-data-aerospike" >> .env'
        sh 'echo  "\nSPRING_DATA_VERSION=${spring_data_version}" >> .env'
        sh './install.sh'
    }
}

def runMvnIntegrationTest(testsTag) {
    setDefaultValuesForChoiceParameters()
    dir("./spring-data-tests") {
        sh "mvn integration-test -Dgroups=${testsTag} -Drp.launch=${JOB_NAME} \
       -Djunit.jupiter.execution.parallel.config.fixed.parallelism=${parallel_classes} \
       -Drp.description=\"spring_data_branch=${spring_data_branch} parallelism=${parallel_classes} Aerospike-Cluster-Version=${asdb_version} Notes=${notes}\" \
       -Dmaven.failsafe.debug=\"-agentlib:jdwp=transport=dt_socket,server=y,suspend=${jvm_suspend},address=*:1045\""
    }
}

def uninstall() {
    sh 'docker rm -vf $(docker ps -aq) || true'
    sh 'docker volume prune -f'
    sh 'docker system prune -f'
}

def cloneAndCheckoutSpringData() {
    dir("/opt/automation") {
        sh 'rm -rf spring-data-aerospike || true'
        sh "git clone https://$GITHUB_TOKEN@github.com/aerospike/spring-data-aerospike.git"
    }
    dir("/opt/automation/spring-data-aerospike") {
        sh 'git checkout ${spring_data_branch}'
    }
}

def setDefaultValuesForChoiceParameters() {
    env.parallel_classes = params.parallel_classes != null ? params.parallel_classes : '5'
    env.asdb_version = params.asdb_version != null ? params.asdb_version : '7.1.0.2'
    env.jvm_suspend = params.jvm_suspend != null ? params.jvm_suspend : 'n'
    env.spring_data_branch = params.spring_data_branch != null ? params.spring_data_branch : 'main'
    env.notes = params.notes != null ? params.notes : 'NO Notes'
    env.spring_data_version = params.spring_data_version != null ? params.spring_data_version : '5.0.0'
}

return this
import groovy.json.JsonSlurper

def getJobsUtils() {
    return load("${pwd()}/devops/backup-service/env/gcp/automation/jenkins/jobs/utils/JobsUtils.groovy")
}

def install() {
    dockerLoginToJfrog()
    cloneAndCheckoutSpringData()
    def SPRING_DATA_PATH = sh(script: 'echo $(pwd)/spring-data-aerospike', returnStdout: true).trim()
    dir("./devops/install/spring-data") {
        sh 'echo  "\nSPRING_DATA_BRANCH=${SPRING_DATA_BRANCH}" >> .env'
        sh 'echo  "\nAEROSPIKE_IMAGE_NAME=${AEROSPIKE_IMAGE_NAME}" >> .env'
        sh 'echo  "\nASDB_VERSION=${ASDB_VERSION}" >> .env'
        sh 'echo  "\nSPRING_DATA_VERSION=${SPRING_DATA_VERSION}" >> .env'
        sh "echo  \"\\nPATH_TO_SPRING_DATA_PROJECT=${SPRING_DATA_PATH}\" >> .env"
        sh './install.sh'
    }
}


def runMvnIntegrationTest() {
    def rpToken = getRPToken()
    dir("./spring-data-tests") {
        sh "mvn integration-test -Dgroups=${TESTS_TAG} -Drp.launch=${TESTS_TAG}/${JOB_NAME} -Drp.api.key=${rpToken} \
       -Djunit.jupiter.execution.parallel.config.fixed.parallelism=${parallel_classes} \
       -Drp.description=\"SPRING_DATA_VERSION=${SPRING_DATA_VERSION} SPRING_DATA_BRANCH=${SPRING_DATA_BRANCH} PARALLEL_CLASSES=${PARALLEL_CLASSES} Aerospike-Cluster-Version=${ASDB_VERSION} NOTES=${NOTES}\""
    }
}

def cloneAndCheckoutSpringData() {
    dir(".") {
        sh 'rm -rf spring-data-aerospike || true'
        sh "git clone https://github.com/aerospike/spring-data-aerospike.git"
    }
    dir("./spring-data-aerospike") {
        sh 'git checkout ${SPRING_DATA_BRANCH}'
    }
}

def getRPToken() {
    getJobsUtils().getRPToken()
}

def uninstall() {
    getJobsUtils().uninstall()
}

def dockerLoginToJfrog() {
    getJobsUtils().dockerLoginToJfrog()
}

def cleanAll() {
    getJobsUtils().cleanAll()
}

return this
import groovy.json.JsonSlurper

def getJobsUtils() {
    return load("${pwd()}/devops/backup-service/env/gcp/automation/jenkins/jobs/utils/JobsUtils.groovy")
}

def install() {
    getJobsUtils().dockerLoginToJfrog()
    dir("./devops/install/cli-backup") {
        sh 'echo  "\nASDB_VERSION=${ASDB_VERSION}" >> .env'
        sh 'echo  "\nBACKUP_BRANCH=${CLI_BACKUP_BRANCH}" >> .env'
        sh 'pwd'
        sh 'ls'
        sh './install.sh'
    }
}

def runMvnIntegrationTest() {
    def testTag = setTestTag()
    def parallelClasses = setParallel()

    dir("./devops/install/cli-backup") {
        sh 'echo  "\nAZURE_CLIENT_ID=${AZURE_CLIENT_ID}" >> .env'
        sh 'echo  "\nAZURE_STORAGE_ACCOUNT=${AZURE_STORAGE_ACCOUNT}" >> .env'
        sh 'echo  "\nAZURE_TENANT_ID=${AZURE_TENANT_ID}" >> .env'
        sh 'echo  "\nAZURE_CLIENT_SECRET=${AZURE_CLIENT_SECRET}" >> .env'
        sh 'echo  "\nGCP_SA_KEY_FILE=${GCP_SA_KEY_FILE}" >> .env'
    }

    def rpToken = getJobsUtils().getRPToken()
    dir("./backup-tests") {
        sh "mvn integration-test -X -Drp.api.key=${rpToken} -Dgroups=${testTag} -Drp.launch=${TEST_TAG}/${JOB_NAME} \
        -DAZURE_STORAGE_ACCOUNT=${AZURE_STORAGE_ACCOUNT} -DAZURE_TENANT_ID=${AZURE_TENANT_ID} -DAZURE_CLIENT_SECRET=${AZURE_CLIENT_SECRET} \
        -DAZURE_CLIENT_ID=${AZURE_CLIENT_ID} -DGCP_SA_KEY_FILE=${GCP_SA_KEY_FILE} \
        -DAZURE_ACCOUNT_NAME=${AZURE_ACCOUNT_NAME} -DAZURE_ACCOUNT_KEY=${AZURE_ACCOUNT_KEY} \
       -DAWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID} -DAWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY} \
       -Djunit.jupiter.execution.parallel.config.fixed.parallelism=${parallelClasses} \
       -Drp.description=\"CLI_BACKUP_BRANCH=${CLI_BACKUP_BRANCH} Aerospike-Source-Version=${ASDB_VERSION}\""
    }
}

def setParallel() {
    def parallelClasses

    if (PARALLEL_CLASSES == null || PARALLEL_CLASSES == 'null') {
        echo "PARALLEL_CLASSES is null or explicitly set to 'null'"
        parallelClasses = '*** PARALLEL CLASSES IS NULL ***'
    } else {
        echo "PARALLEL_CLASSES has a value: ${PARALLEL_CLASSES}"
        parallelClasses = PARALLEL_CLASSES
    }
    echo "parallelClasses resolved as: ${parallelClasses}"

    // Debugging: Print the exact value of TEST_TAG
    echo "Original TEST_TAG value: '${env.TEST_TAG}'"

    // Ensure TEST_TAG is trimmed and handled properly
    def testTag = (env.TEST_TAG ?: '').trim()
    echo "Trimmed TEST_TAG value: '${testTag}'"

    // Update parallelClasses if TEST_TAG matches specific values
    if (testTag == 'CLI-BACKUP-SEQUENTIAL' || testTag == 'CLI-BACKUP-C-TO-GO' || testTag == 'CLI-BACKUP-GO-TO-C' || testTag == 'CLI-3-NODES-CLUSTER') {
        parallelClasses = "1"
        echo "parallelClasses updated to: ${parallelClasses} due to TEST_TAG being sequential"
    }

    return parallelClasses
}

def setTestTag() {
    echo "Original TEST_TAG value: '${env.TEST_TAG}'"
    def testTag

    // Handle null or explicitly 'null' values
    if (env.TEST_TAG == null || env.TEST_TAG == 'null') {
        echo "TEST_TAG is null or explicitly set to 'null'"
        testTag = '*** TEST TAG IS NULL ***'
    } else {
        echo "TEST_TAG has a value: '${env.TEST_TAG}'"
        testTag = env.TEST_TAG.trim() // Trim whitespace
    }
    echo "Final test tag resolved as: '${testTag}'"

    dir("./devops/install/cli-backup") {
        if (testTag == 'CLI-BACKUP-C-TO-GO') {
            sh "echo 'BACKUP_METHOD=c_backup' >> .env"
            echo "Set BACKUP_METHOD to c_backup"
            sh "echo 'RESTORE_METHOD=go_restore' >> .env"
            echo "Set RESTORE_METHOD to go_restore"
        } else if (testTag == 'CLI-BACKUP-GO-TO-C') {
            sh "echo 'BACKUP_METHOD=go_backup' >> .env"
            echo "Set BACKUP_METHOD to go_backup"

            sh "echo 'RESTORE_METHOD=c_restore' >> .env"
            echo "Set RESTORE_METHOD to c_restore"

            testTag = "CLI-BACKUP-C-TO-GO"
            echo "Test tag updated to: CLI-BACKUP-C-TO-GO"
        } else {
            echo "TEST_TAG does not match any expected value."
        }
    }
    echo "Final test tag: '${testTag}'"
    return testTag
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

def fetchNetworkDetails() {
    getJobsUtils().fetchNetworkDetails()
}

def installMultiNodeENV() {
    getJobsUtils().installMultiNodeENV()
}

def removeMultiNodeENV() {
    getJobsUtils().removeMultiNodeENV()
}

def runTestOnLocalTlsEnv(String test_tag) {
    def parallelClasses = setParallel()
    dir("./devops/install/cli-backup") {
        sh 'echo "\nLOCAL_TLS_ENABLED=${ASDB_TLS_ENABLED}" >> .env'
        sh 'echo "\nIS_RUNNING_ON_LOCAL_3_NODES_ENV=true" >> .env'
        sh 'echo  "\nAZURE_CLIENT_ID=${AZURE_CLIENT_ID}" >> .env'
        sh 'echo  "\nAZURE_STORAGE_ACCOUNT=${AZURE_STORAGE_ACCOUNT}" >> .env'
        sh 'echo  "\nAZURE_TENANT_ID=${AZURE_TENANT_ID}" >> .env'
        sh 'echo  "\nAZURE_CLIENT_SECRET=${AZURE_CLIENT_SECRET}" >> .env'
        sh 'echo  "\nGCP_SA_KEY_FILE=${GCP_SA_KEY_FILE}" >> .env'
    }

    install()

    def rpToken = getJobsUtils().getRPToken()
    dir("./backup-tests") {
        sh "mvn integration-test -X -Drp.api.key=${rpToken} -Dgroups=${test_tag} -Drp.launch=${test_tag}/${JOB_NAME} -DTLS_ENABLED=${ASDB_TLS_ENABLED} \
        -DAZURE_STORAGE_ACCOUNT=${AZURE_STORAGE_ACCOUNT} -DAZURE_TENANT_ID=${AZURE_TENANT_ID} -DAZURE_CLIENT_SECRET=${AZURE_CLIENT_SECRET} \
        -DAZURE_CLIENT_ID=${AZURE_CLIENT_ID} -DGCP_SA_KEY_FILE=${GCP_SA_KEY_FILE} \
        -DAZURE_ACCOUNT_NAME=${AZURE_ACCOUNT_NAME} -DAZURE_ACCOUNT_KEY=${AZURE_ACCOUNT_KEY} \
       -DAWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID} -DAWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY} \
       -DSECRET_AGENT_IP=${env.SECRET_AGENT_IP} -DSECRET_AGENT_PORT=${env.SECRET_AGENT_PORT} -DASDB_IP=${env.ASDB_IP} -DASDB_PORT=${env.ASDB_PORT} \
       -Djunit.jupiter.execution.parallel.config.fixed.parallelism=${parallelClasses} \
       -Drp.description=\"CLI_BACKUP_BRANCH=${CLI_BACKUP_BRANCH} Aerospike-Source-Version=${ASDB_VERSION} ASDB_SIZE=${ASDB_SIZE} \
        PARALLEL_CLASSES=${PARALLEL_CLASSES} ASDB_VERSION=${ASDB_VERSION} \""
        //PARALLEL_CLASSES=${PARALLEL_CLASSES} ASDB_VERSION=${ASDB_VERSION} TLS_ENABLED=${TLS_ENABLED} \""
    }
}

def runMvnIntegrationTestGCP(String test_tag) {
    echo "Starting runMvnIntegrationTestGCP"
    dir("./devops/install/cli-backup") {
        sh 'echo  "\nIS_RUNNING_ON_LOCAL_3_NODES_ENV=true" >> .env'
    }
    install()
    def parallelClasses = setParallel()
    dir("./devops/install/cli-backup") {
        sh 'echo "TLS_ENABLED=true" >> .env'
        sh 'echo "LOAD_LEVEL=${LOAD_LEVEL}" >> .env'
    }

    def rpToken = getRPToken()
    dir("./backup-tests") {
        sh "mvn integration-test -Drp.api.key=${rpToken} -Dgroups=${test_tag} -Drp.launch=${test_tag}/${JOB_NAME} -Dqa_environment=GCP \
        -Djunit.jupiter.execution.parallel.config.fixed.parallelism=${parallelClasses} \
        -DAZURE_STORAGE_ACCOUNT=${AZURE_STORAGE_ACCOUNT} -DAZURE_TENANT_ID=${AZURE_TENANT_ID} -DAZURE_CLIENT_SECRET=${AZURE_CLIENT_SECRET} \
        -DAZURE_CLIENT_ID=${AZURE_CLIENT_ID} -DGCP_SA_KEY_FILE=${GCP_SA_KEY_FILE} \
        -DAZURE_ACCOUNT_NAME=${AZURE_ACCOUNT_NAME} -DAZURE_ACCOUNT_KEY=${AZURE_ACCOUNT_KEY} \
        -DAWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID} -DAWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY} \
        -Dmaven.failsafe.debug=\"-agentlib:jdwp=transport=dt_socket,server=y,suspend=${JVM_SUSPEND},address=*:1045\" \
        -Drp.description=\"ENV_WORKSPACE=${ENV_WORKSPACE} \
         Aerospike-Source-Version=${ASDB_VERSION} \""
    }
}

return this
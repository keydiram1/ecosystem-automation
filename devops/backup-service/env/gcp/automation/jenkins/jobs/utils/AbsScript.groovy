import groovy.json.JsonSlurper

def getJobsUtils() {
    return load("${pwd()}/devops/backup-service/env/gcp/automation/jenkins/jobs/utils/JobsUtils.groovy")
}

def install() {
    dockerLoginToJfrog()
    dir("./devops/install/abs") {
        sh 'echo  "\nASDB_VERSION=${ASDB_VERSION}" >> .env'
        sh 'echo  "\nPATH_TO_ABS_PROJECT=/opt/automation/localInstallation/aerospike-backup-service" >> .env'
        sh 'echo  "\nABS_BRANCH=${ABS_BRANCH}" >> .env'
        sh 'echo  "\nABS_VERSION=${ABS_VERSION}" >> .env'
        if (env.PULL_FROM_JFROG.toString() == 'true') {
            sh 'echo  "\nPULL_FROM_JFROG=true" >> .env'
            sh 'echo  "\nABS_JFROG_REPOSITORY_STAGE=${abs_jfrog_stage}" >> .env'
        }
        if (env.TEST_TAG.toString() == 'ABS-SERVICE-TEST') {
            sh "./editService.sh"
        }
        if (env.TEST_TAG.toString() == 'ABS-LOCAL-LOAD-TEST') {
            sh "echo 'SERVICE_CONF_FILE=configLoad.yml' >> .env"
            sh "echo 'INSTALL_TWO_SOURCE_CLUSTERS=true' >> .env"
        }
        sh 'echo  "\nCONFIGURATION_FILE=${ABS_CONFIGURATION_FILE}" >> .env'
        if (env.ABS_CONFIGURATION_FILE.toString() == 's3') {
            sh "echo 'SERVICE_CONF_FILE=configRemoteS3.yml' >> .env"
        }
        sh './install.sh'
    }
}

def runMvnIntegrationTest(rpDescription) {
    dir("./devops/install/abs") {
        sh 'echo  "\nCONFIG_BACKUP_PARALLEL=${CONFIG_BACKUP_PARALLEL}" >> .env'
        sh 'echo  "\nCONFIG_RESTORE_PARALLEL=${CONFIG_RESTORE_PARALLEL}" >> .env'
        sh 'echo  "\nABS_STORAGE_PROVIDER=${ABS_STORAGE_PROVIDER}" >> .env'
    }
    def testTag = setTestTag()
    def parallelClasses = setParallel(testTag)
    def rpToken = getRPToken()
    dir("./backup-tests") {
        sh "mvn integration-test -X -Drp.api.key=${rpToken} -Dgroups=${testTag} -Drp.launch=${TEST_TAG}/${JOB_NAME} -DAZURE_CLIENT_ID=${AZURE_CLIENT_ID} \
       -DAZURE_STORAGE_ACCOUNT=${AZURE_STORAGE_ACCOUNT} -DAZURE_TENANT_ID=${AZURE_TENANT_ID} -DAZURE_CLIENT_SECRET=${AZURE_CLIENT_SECRET} \
       -Djunit.jupiter.execution.parallel.config.fixed.parallelism=${parallelClasses} -DGCP_SA_KEY_FILE=${GCP_SA_KEY_FILE} \
       -DAWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID} -DAWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY} -Dstorage_provider=${ABS_STORAGE_PROVIDER} \
       -Drp.description=\"${rpDescription} abs_branch=${ABS_BRANCH} storage_provider=${ABS_STORAGE_PROVIDER} abs_version=${ABS_VERSION} \
       config_backup_parallel=${CONFIG_BACKUP_PARALLEL} config_restore_parallel=${CONFIG_RESTORE_PARALLEL} \
       Aerospike-Source-Version=${ASDB_VERSION} abs-configuration-file=${ABS_CONFIGURATION_FILE} \""
    }
}

def runMvnIntegrationTest() {
    runMvnIntegrationTest("")
}

def installGCP() {
    dir("./devops/env/gcp/stage") {
        sh "envsubst < common_vars.tpl.yaml > common_vars.yaml"
        sh "task apply"
    }
}

def runMvnIntegrationTestGCP(String test_tag) {
    def parallelClasses = setParallel(test_tag)
    dir("./devops/install/abs") {
        sh 'echo "TLS_ENABLED=true" >> .env'
        sh 'echo "LOAD_LEVEL=${LOAD_LEVEL}" >> .env'
        sh 'echo "TEST_DURATION=${TEST_DURATION}" >> .env'
        sh 'echo "ON_GOING_THROUGHPUT=${ON_GOING_THROUGHPUT}" >> .env'
        sh 'echo "DATA_SPIKES_DURATION=${DATA_SPIKES_DURATION}" >> .env'
    }

    def rpToken = getRPToken()
    dir("./backup-tests") {
        sh "mvn integration-test -Drp.api.key=${rpToken} -Dgroups=${test_tag} -Drp.launch=${test_tag}/${JOB_NAME} -Dqa_environment=GCP \
        -Djunit.jupiter.execution.parallel.config.fixed.parallelism=${parallelClasses} \
        -DAZURE_STORAGE_ACCOUNT=${AZURE_STORAGE_ACCOUNT} -DAZURE_TENANT_ID=${AZURE_TENANT_ID} -DAZURE_CLIENT_SECRET=${AZURE_CLIENT_SECRET} \
        -DAZURE_CLIENT_ID=${AZURE_CLIENT_ID} -DGCP_SA_KEY_FILE=${GCP_SA_KEY_FILE} \
        -DAWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID} -DAWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY} \
        -Dmaven.failsafe.debug=\"-agentlib:jdwp=transport=dt_socket,server=y,suspend=${JVM_SUSPEND},address=*:1045\" \
        -Drp.description=\"ENV_WORKSPACE=${ENV_WORKSPACE} STORAGE_PROVIDER=${ABS_STORAGE_PROVIDER} ABS_VERSION=${ABS_IMAGE_TAG} \
         DATA_TYPE=${DATA_TYPE} NUMBER_OF_RECORDS_IN_MILLIONS=${NUMBER_OF_RECORDS_IN_MILLIONS} \
         Aerospike-Source-Version=${ASDB_VERSION} \""
    }
}

def setTestTag() {
    echo "Original TEST_TAG value: '${env.TEST_TAG}'"
    def testTag

    if (env.TEST_TAG == null || env.TEST_TAG == 'null') {
        echo "TEST_TAG is null or explicitly set to 'null'"
        testTag = '*** TEST TAG IS NULL ***'
    } else {
        echo "TEST_TAG has a value: '${env.TEST_TAG}'"
        testTag = env.TEST_TAG.trim()
    }
    echo "Final test tag resolved as: '${testTag}'"

    dir("./devops/install/abs") {
        if (env.ABS_CONFIGURATION_FILE.toString() == 'http') {
            echo "ABS_CONFIGURATION_FILE is set to http"
            testTag = 'ABS-SLOW-BACKUP'
            echo "Test tag updated to: ABS-SLOW-BACKUP"
        } else if (testTag == 'ABS-C-TO-GO') {
            sh "echo 'BACKUP_METHOD=asbackup' >> .env"
            echo "Set BACKUP_METHOD to asbackup"
            sh "echo 'RESTORE_METHOD=backup_service' >> .env"
            echo "Set RESTORE_METHOD to backup_service"
        } else if (testTag == 'ABS-GO-TO-C') {
            sh "echo 'BACKUP_METHOD=backup_service' >> .env"
            echo "Set BACKUP_METHOD to backup_service"

            sh "echo 'RESTORE_METHOD=asrestore' >> .env"
            echo "Set RESTORE_METHOD to asrestore"

            testTag = "ABS-C-TO-GO"
            echo "Test tag updated to: ABS-C-TO-GO"
        } else {
            echo "TEST_TAG does not match any expected value."
        }
    }
    echo "Final test tag: '${testTag}'"
    return testTag
}

def setParallel(String test_tag) {
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
    echo "Original TEST_TAG value: '${test_tag}'"

    // Ensure TEST_TAG is trimmed and handled properly
    def testTag = (test_tag ?: '').trim()
    echo "Trimmed TEST_TAG value: '${testTag}'"

    // Update parallelClasses if TEST_TAG matches specific values
    if (testTag == 'ABS-C-TO-GO' || testTag == 'ABS-SEQUENTIAL-TESTS' || testTag == 'ABS-SEQUENTIAL-TESTS-2' || testTag == 'ABS-GO-TO-C') {
        parallelClasses = "1"
        echo "parallelClasses updated to: ${parallelClasses} due to TEST_TAG being sequential"
    }

    return parallelClasses
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
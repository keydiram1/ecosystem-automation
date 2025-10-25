def parallel_classes
def asdb_version
def jvm_suspend
def abs_branch
def abs_version
def config_restore_parallel
def storage_provider

def uninstall() {
    sh 'docker rm -vf $(docker ps -aq) || true'
    sh 'docker volume prune -f'
    sh 'docker system prune -f'
}

def install(configFile) {
    setDefaultValuesForChoiceParameters()
    dir("./devops/install/abs") {
        sh 'echo  "\nSTORAGE_PROVIDER=${storage_provider}" >> .env'
        sh 'echo  "\nASDB_VERSION=${asdb_version}" >> .env'
        sh 'echo  "\nPATH_TO_ABS_PROJECT=/opt/automation/localInstallation/aerospike-backup-service" >> .env'
        sh 'echo  "\nABS_BRANCH=${abs_branch}" >> .env'
        sh 'echo  "\nCONFIG_RESTORE_PARALLEL=${config_restore_parallel}" >> .env'
        sh "echo  '\nCONFIGURATION_FILE=${configFile}' >> .env"
        if (env.pull_from_jfrog.toString() == 'true') {
            sh 'echo  "\nPULL_FROM_JFROG=true" >> .env'
            sh 'echo  "\nABS_VERSION=${abs_version}" >> .env'
            sh 'echo  "\nABS_JFROG_REPOSITORY_STAGE=${abs_jfrog_stage}" >> .env'
        }
        sh './install.sh'
    }
}

def install() {
    install('local')
}

def runMvnIntegrationTest(testsTag, rpDescription) {
    setDefaultValuesForChoiceParameters()
    dir("./backup-tests") {
        sh "mvn integration-test -Dgroups=${testsTag} -Drp.launch=${JOB_NAME} \
       -Djunit.jupiter.execution.parallel.config.fixed.parallelism=${parallel_classes} \
       -Drp.description=\"${rpDescription} storage_provider=${storage_provider} abs_branch=${abs_branch} abs_version=${abs_version} \
       config_restore_parallel=${config_restore_parallel} \
       Aerospike-Source-Version=${asdb_version}\" \
       -Dmaven.failsafe.debug=\"-agentlib:jdwp=transport=dt_socket,server=y,suspend=${jvm_suspend},address=*:1045\""
    }
}

def runMvnIntegrationTest(testsTag) {
    runMvnIntegrationTest(testsTag, "")
}

def installAws() {
    cloneAutomationProjectAndCheckout()
    dir("/opt/automation/abs/absAwsInstall/ecosystem-automation/devops/backup-service/env/aws") {
        sh "task set:cluster:size -- ${number_of_nodes}"
        sh "task set:workspace -- ${aws_env_workspace}"
        sh "task set:service_version -- ${abs_version}"
        sh "task apply"
    }
}

def uninstallAws() {
    cloneAutomationProjectAndCheckout()
    dir("/opt/automation/abs/absAwsInstall/ecosystem-automation/devops/backup-service/env/aws") {
        sh "task set:workspace -- ${aws_env_workspace}"
        sh "task plan"
        sh "task destroy"
    }
}

def runMvnIntegrationTestAWS(testsTag) {
    def clusterInstanceIds, backupServiceUrl, clusterSshKey, rootCA;
    cloneAutomationProjectAndCheckout()
    dir("/opt/automation/abs/absAwsInstall/ecosystem-automation/devops/backup-service/env/aws") {
        sh "task set:workspace -- ${aws_env_workspace}"
        sh "task plan"
        clusterInstanceIds = getTerraformOutput("cluster_instance_ids").join(',')
        sh "echo clusterInstanceIds: ${clusterInstanceIds}"
        backupServiceUrl = getTerraformOutput("ingress_hostname")
        backupServiceUrl = backupServiceUrl.join(", ").replaceAll("\\[|\\]", "") // Remove square brackets
        sh "echo backupServiceUrl: ${backupServiceUrl}"
        sh 'task output:cluster_ssh_key >> clusterSshKey.pem'
        sh 'chmod 400 clusterSshKey.pem'
        sh 'task output:root_ca_pem:jks'
        clusterSshKey = "/opt/automation/abs/absAwsInstall/ecosystem-automation/devops/backup-service/env/aws/clusterSshKey.pem"
        rootCA = "/opt/automation/abs/absAwsInstall/ecosystem-automation/devops/backup-service/env/aws/root.ca.jks"
    }
    dir("./devops/install/abs") {
        sh 'echo  "\nTLS_ENABLED=true" >> .env'
    }
    def load_level = params.load_level != null ? params.load_level : 'TheLoadLevelHasNotBeenSet'
    def test_duration = params.test_duration != null ? params.test_duration : 'test_durationHasNotBeenSet'
    def on_going_throughput = params.on_going_throughput != null ? params.on_going_throughput : 'on_going_throughputHasNotBeenSet'
    def data_spikes_duration = params.data_spikes_duration != null ? params.data_spikes_duration : 'data_spikes_durationHasNotBeenSet'

    dir("./backup-tests") {
        sh "mvn integration-test -Dgroups=${testsTag} -Drp.launch=${JOB_NAME} -Dqa_environment=AWS -Daws_env_workspace=${aws_env_workspace} \
        -DclusterInstanceIds=${clusterInstanceIds} -DbackupServiceUrl=${backupServiceUrl} -DclusterSshKey=${clusterSshKey} -DrootCA=${rootCA} \
        -Djunit.jupiter.execution.parallel.config.fixed.parallelism=${parallel_classes} -Dload_level=${load_level} \
        -Dtest_duration=${test_duration} -Don_going_throughput=${on_going_throughput} -Ddata_spikes_duration=${data_spikes_duration} \
        -Drp.description=\"Running on AWS ABS-Version=${abs_version} aws_env_workspace=${aws_env_workspace}\" \
        -Dmaven.failsafe.debug=\"-agentlib:jdwp=transport=dt_socket,server=y,suspend=${jvm_suspend},address=*:1045\""
    }
}


def cloneAutomationProjectAndCheckout() {
    dir("/opt/automation/abs/absAwsInstall") {
        sh 'rm -rf ecosystem-automation || true'
        sh "git clone https://${GITHUB_TOKEN}@github.com/citrusleaf/ecosystem-automation.git"
    }
    dir("/opt/automation/abs/absAwsInstall/ecosystem-automation") {
        sh 'git checkout ${automation_install_branch}'
    }
}

def getTerraformOutput(propertyToGet) {
    def output = sh(
            returnStdout: true,
            script: """task output:${propertyToGet}"""
    ).trim()
    output = output.replaceAll('\\s+', '')        // Remove spaces
    output = output.replaceAll('\\[,', '[')      // Remove space after comma
    output = output.replaceAll('[\\[\\]]', '')  // Remove square brackets
    return output.tokenize(',')
}

def setDefaultValuesForChoiceParameters() {
    env.parallel_classes = params.parallel_classes != null ? params.parallel_classes : '15'
    env.asdb_version = params.asdb_version != null ? params.asdb_version : '5.7.0.31'
    env.jvm_suspend = params.jvm_suspend != null ? params.jvm_suspend : 'n'
    env.abs_branch = params.abs_branch != null ? params.abs_branch : 'v3'
    env.abs_version = params.abs_version != null ? params.abs_version : 'latest'
    env.config_restore_parallel = params.config_restore_parallel != null ? params.config_restore_parallel : '1'
    env.storage_provider = params.storage_provider != null ? params.storage_provider : 'LOCAL'
}

return this
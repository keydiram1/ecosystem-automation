def parallel_classes
def asdb_version
def jvm_suspend
def asbackup_branch

def uninstall() {
    sh 'docker rm -vf $(docker ps -aq) || true'
    sh 'docker volume prune -f'
    sh 'docker system prune -f'
}

def install(configFile) {
    setDefaultValuesForChoiceParameters()
    dir("./devops/install/cli-backup") {
        sh 'echo  "\nASDB_VERSION=${asdb_version}" >> .env'
        sh 'echo  "\nBACKUP_BRANCH=${asbackup_branch}" >> .env'
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
       -Drp.description=\"${rpDescription} asbackup_branch=${asbackup_branch} \
       Aerospike-Source-Version=${asdb_version}\" \
       -Dmaven.failsafe.debug=\"-agentlib:jdwp=transport=dt_socket,server=y,suspend=${jvm_suspend},address=*:1045\""
    }
}

def runMvnIntegrationTest(testsTag) {
    runMvnIntegrationTest(testsTag, "")
}

def setDefaultValuesForChoiceParameters() {
    env.parallel_classes = params.parallel_classes != null ? params.parallel_classes : '15'
    env.asdb_version = params.asdb_version != null ? params.asdb_version : '7.1.0.2'
    env.jvm_suspend = params.jvm_suspend != null ? params.jvm_suspend : 'n'
    env.asbackup_branch = params.asbackup_branch != null ? params.asbackup_branch : 'main'
}

def runMvnIntegrationTestAWS(testsTag) {
    def clusterInstanceIds, backupServiceUrl, clusterSshKey, rootCA;
    cloneAutomationProjectAndCheckout()
    dir("/opt/automation/abs/absAwsInstall/ecosystem-automation/devops/backup-service/env/aws") {
        sh "task set:workspace -- test"
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

    dir("./backup-tests") {
        sh "mvn integration-test -Dgroups=${testsTag} -Drp.launch=${JOB_NAME} -Dqa_environment=AWS -Daws_env_workspace=test \
        -DclusterInstanceIds=${clusterInstanceIds} -DbackupServiceUrl=${backupServiceUrl} -DclusterSshKey=${clusterSshKey} -DrootCA=${rootCA} \
        -Djunit.jupiter.execution.parallel.config.fixed.parallelism=${parallel_classes} \
        -Dmaven.failsafe.debug=\"-agentlib:jdwp=transport=dt_socket,server=y,suspend=${jvm_suspend},address=*:1045\""
    }
}

def installAws() {
    cloneAutomationProjectAndCheckout()
    dir("/opt/automation/abs/absAwsInstall/ecosystem-automation/devops/backup-service/env/aws") {
        sh "task set:cluster:size -- 1"
        sh "task set:workspace -- test"
        sh "task set:service_version -- tests"
        sh "task apply"
    }
}

def uninstallAws() {
    cloneAutomationProjectAndCheckout()
    dir("/opt/automation/abs/absAwsInstall/ecosystem-automation/devops/backup-service/env/aws") {
        sh "task set:workspace -- test"
        sh "task plan"
        sh "task destroy"
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

return this
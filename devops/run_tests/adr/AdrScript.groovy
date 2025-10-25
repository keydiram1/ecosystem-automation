def parallel_classes
def asdb_version
def aerospike_backup_image_version
def jvm_suspend
def adr_image_version
def static_configuration
def tls_enabled
def jfrog_repository_stage
def aws_env_workspace

def uninstall() {
    sh 'docker rm -vf $(docker ps -aq) || true'
    sh 'docker volume prune -f'
    sh 'docker system prune -f'
    sh 'docker buildx prune -f'
}

def install() {
    setDefaultValuesForChoiceParameters()
    dir("./devops/install/backup") {
        sh 'cp -f conf/slave/.env .'
        sh 'echo  "\nASDB_VERSION=${asdb_version}" >> .env'
        sh 'echo  "\nAEROSPIKE_BACKUP_IMAGE_VERSION=${aerospike_backup_image_version}" >> .env'
        if (env.tls_enabled.toString() == 'true')
            sh 'echo  "\nTLS_ENABLED=${tls_enabled}" >> .env'
        if (env.adr_image_version)
            sh 'echo  "\nBACKUP_IMAGES_VERSION=${adr_image_version}" >> .env'
        if (env.static_configuration.toString() == 'false')
            sh 'echo  "\nSTATIC_CONFIGURATION=false" >> .env'
        sh "sed -i '0,/ADR_JFROG_REPOSITORY_STAGE.*/s//ADR_JFROG_REPOSITORY_STAGE=${jfrog_repository_stage}/' .env"
        if (env.install_from_enterprise_backup_project.toString() == 'true') {
            sh 'echo  "\nPULL_FROM_DOCKER_REPOSITORY=false" >> .env'
            if (env.delete_enterprise_backup_images.toString() == 'true')
                deleteAllImages()
            dir("/opt/automation/localInstallation") {
                sh 'rm -rf enterprise-backup || true'
                sh "git clone https://$GITHUB_TOKEN@github.com/citrusleaf/enterprise-backup.git"
                dir("/opt/automation/localInstallation/enterprise-backup") {
                    sh 'git checkout ${enterprise_backup_branch}'
                }
            }
            sh 'cp -f conf/slave/docker-compose.local.yml /opt/automation/localInstallation/enterprise-backup/docker-compose.local.yml'
        }
        sh './install.sh'
    }

}

def runMvnIntegrationTest(testsTag) {
    setDefaultValuesForChoiceParameters()
    def multinodeSrc = ""
    if (env.multinode_source)
        multinodeSrc = "multinode_source=${multinode_source}"
    dir("./backup-tests") {
        sh ". /etc/environment; mvn integration-test -Dgroups=${testsTag} -Drp.launch=${JOB_NAME} -Dqa_environment=LOCAL -Daws_env_workspace=${aws_env_workspace} \
       -Djunit.jupiter.execution.parallel.config.fixed.parallelism=${parallel_classes} \
       -Drp.description=\"ADR-Version=\"${adr_image_version}\" enterprise_backup_branch=${enterprise_backup_branch} \
        install_from_enterprise_backup_project=${install_from_enterprise_backup_project} \
       ${multinodeSrc} static_configuration=${static_configuration} jfrog_repository_stage=${jfrog_repository_stage} \
       Aerospike-Source-Version=${asdb_version} Aerospike-Backup-Version=${aerospike_backup_image_version} TLS-Enabled=${tls_enabled}\" \
       -Dmaven.failsafe.debug=\"-agentlib:jdwp=transport=dt_socket,server=y,suspend=${jvm_suspend},address=*:1045\""
    }
}

def installAws(numberOfAsNodes) {
    dir("/opt/automation/awsInstall") {
        cloneEnterpriseBackupForAws()
    }
    dir("/opt/automation/awsInstall/enterprise-backup/helm/aerospike-enterprise-backup/config") {
        sh "sed -i 's/backupIntervalSeconds.*/backupIntervalSeconds: 5/g' smd-handler.yml"
        sh "sed -i 's/xdrSchedulerIntervalMs.*/xdrSchedulerIntervalMs: 1000/g' xdr-scheduler.yml"
        sh "sed -i 's/xdrSchedulerCheckLagIntervalMs.*/xdrSchedulerCheckLagIntervalMs: 1000/g' xdr-scheduler.yml"
        sh "sed -i 's/compactIntervalSeconds.*/compactIntervalSeconds: 5/g' compactor.yml"
        sh "sed -i 's/compactIntervalSeconds.*/compactIntervalSeconds: 5/g' compactor.yml"
        sh "sed -i 's/smdCompactIntervalSeconds.*/smdCompactIntervalSeconds: 10/g' compactor.yml"
        sh "sed -i 's/deleteOldRecordsIntervalSeconds.*/deleteOldRecordsIntervalSeconds: 5/g' compactor.yml"
        sh """sed -i 's/"backup-cluster"/"backup.cluster.adr.${aws_env_workspace}.internal"/g' xdr-transformer.yml"""
    }

    dir("/opt/automation/awsInstall/enterprise-backup/devops/infra/envs/aws") {
        sh "task set-worspace -- ${aws_env_workspace}"
    }

    dir("/opt/automation/awsInstall/enterprise-backup/devops/infra/envs/aws") {
        sh "yq e '.aerospike.backup.size = ${numberOfAsNodes}' -i common_vars.yaml"
        sh "yq e '.aerospike.source.size = ${numberOfAsNodes}' -i common_vars.yaml"
    }

    dir("/opt/automation/awsInstall/enterprise-backup/devops/infra/envs/aws/values") {
        sh "sed -i 's/tag: \"latest\".*/tag: ${adr_version}/g' adr-values.yaml"
    }

    setTlsIfEnabled()

    dir("/opt/automation/awsInstall/enterprise-backup/devops/infra/envs/aws") {
        sh "eval `ssh-agent -s`; task apply"
    }
}

def installAws() {
    installAws(1)
}

def uninstallAws() {
    cloneEnterpriseBackupForAws()
    setTlsIfEnabled()
    dir("/opt/automation/awsInstall/enterprise-backup/devops/infra/envs/aws") {
        sh "task init"
        sh "task set-worspace -- ${aws_env_workspace}"
        sh "task destroy"
    }
}

def runMvnIntegrationTestAWS(testsTag, asbenchDurationSeconds) {
    cloneEnterpriseBackupForAws()
    setTlsIfEnabled()
    dir("/opt/automation/awsInstall/enterprise-backup/devops/infra/envs/aws") {
        sh "task set-worspace -- ${aws_env_workspace}"
        sh "task init"
    }
    if (env.tls_enabled.toString() == 'true') {
        dir("./devops/install/backup") {
            sh 'echo  "\nTLS_ENABLED=${tls_enabled}" >> .env'
        }
    }
    dir("./devops/install/backup") {
        if (env.dynamic_xdr_mode.toString() == 'true')
            sh 'echo  "\nSTATIC_CONFIGURATION=false" >> .env'
    }

    def backupInstanceIds, sourceInstanceIds, restBackendUrl, vaultIP, vaultToken;
    dir("/opt/automation/awsInstall/enterprise-backup/devops/infra/envs/aws") {
        backupInstanceIds = getTerraformOutput("backup_instance_ids").join(',')
        sh "echo backupInstanceIds: ${backupInstanceIds}"
        sourceInstanceIds = getTerraformOutput("source_instance_ids").join(',')
        sh "echo sourceInstanceIds: ${sourceInstanceIds}"
        restBackendUrl = getTerraformOutput("ingress_hostname")
        restBackendUrl = restBackendUrl.join(", ").replaceAll("\\[|\\]", "")  // Remove square brackets
        sh "echo restBackendUrl: ${restBackendUrl}"
        vaultIP = getTerraformOutput("vault_public_ip").join(',')
        sh "echo vaultIP: ${vaultIP}"
        vaultToken = getTerraformOutput("vault_token").join(',')
        sh "echo vaultToken: ${vaultToken}"
    }

    dir("./backup-tests") {
        sh ". /etc/environment; mvn integration-test -Dgroups=${testsTag} -Drp.launch=${JOB_NAME} -Dqa_environment=AWS -Daws_env_workspace=${aws_env_workspace} \
        -DbackupInstanceIds=${backupInstanceIds} -DsourceInstanceIds=${sourceInstanceIds} -DrestBackendUrl=${restBackendUrl} \
        -DvaultIP=${vaultIP} -DvaultToken=${vaultToken} \
       -Djunit.jupiter.execution.parallel.config.fixed.parallelism=${parallel_classes} -Dasbench_duration_seconds=${asbenchDurationSeconds} \
        -Drp.description=\"Running on AWS ADR-Version=${adr_version} dynamic_xdr_mode=${dynamic_xdr_mode} aws_env_workspace=${aws_env_workspace}\" \
       -Dmaven.failsafe.debug=\"-agentlib:jdwp=transport=dt_socket,server=y,suspend=${jvm_suspend},address=*:1045\""
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

def runMvnIntegrationTestAWS(testsTag) {
    runMvnIntegrationTestAWS(testsTag, 1)
}

def runSingleTestSuite() {
    def testName = ""
    if (env.TEST_NAME.toString() != "")
        testName = "#${TEST_NAME}"
    def testSuite = "-Dit.test=${CLASS_QUALIFIED_NAME}${testName}"
    dir("./backup-tests") {
        sh ". /etc/environment; mvn integration-test -Drp.launch=${JOB_NAME} -Dqa_environment=${qa_environment} ${testSuite} \
        -Drp.description=\"Single test suite running on ${qa_environment} installation\" \
       -Dmaven.failsafe.debug=\"-agentlib:jdwp=transport=dt_socket,server=y,suspend=${jvm_suspend},address=*:1045\""
    }
}

def configQueueHandlerForQueueRecoverTest() {
    dir("./devops/install/backup/docker") {
        sh 'echo  "\nqueuePullingIntervalMs: 60000" >> queue-handler-config.yml'
        sh 'echo  "\nzombieRecoverIntervalMs: 150000" >> queue-handler-config.yml'
        sh 'echo  "\nrecordRecoverThresholdMs: 3000" >> queue-handler-config.yml'
    }
}

def deleteAllImages() {
    sh 'docker rm -vf $(docker ps -aq) || true'
    sh 'docker image rm enterprise-backup-rest-backend | true'
    sh 'docker image rm enterprise-backup-compactor | true'
    sh 'docker image rm enterprise-backup-queue-handler | true'
    sh 'docker image rm enterprise-backup-smd-handler | true'
    sh 'docker image rm enterprise-backup-xdr-scheduler | true'
    sh 'docker image rm enterprise-backup-xdr-transformer | true'
    sh 'docker image rm enterprise-backup-authenticator | true'
    sh 'docker image rm enterprise-backup-storage-provider | true'
    sh 'docker system prune -f | true'
}

def setDefaultValuesForChoiceParameters() {
    parallel_classes = params.parallel_classes != null ? params.parallel_classes : '15'
    asdb_version = params.asdb_version != null ? params.asdb_version : '5.7.0.31'
    aerospike_backup_image_version = params.aerospike_backup_image_version != null ? params.aerospike_backup_image_version : '6.4.0.1'
    jvm_suspend = params.jvm_suspend != null ? params.jvm_suspend : 'n'
    adr_image_version = params.adr_image_version != null ? params.adr_image_version : 'latest'
    static_configuration = params.static_configuration != null ? params.static_configuration : 'true'
    tls_enabled = params.tls_enabled != null ? params.tls_enabled : 'false'
    jfrog_repository_stage = params.jfrog_repository_stage != null ? params.jfrog_repository_stage : 'dev'
    aws_env_workspace = params.aws_env_workspace != null ? params.aws_env_workspace : 'noAwsWorkspaceGivenFromTheJob'
}

def cloneEnterpriseBackupForAws() {
    dir("/opt/automation/awsInstall") {
        sh 'rm -rf enterprise-backup || true'
        sh "git clone https://$GITHUB_TOKEN@github.com/citrusleaf/enterprise-backup.git"
    }
    dir("/opt/automation/awsInstall/enterprise-backup") {
        sh 'git checkout ${enterprise_backup_branch}'
    }
}

def setTlsIfEnabled() {
    if (env.tls_enabled.toString() == 'true') {
        dir("./devops/install/backup") {
            sh 'echo  "\nTLS_ENABLED=${tls_enabled}" >> .env'
        }
        dir("/opt/automation/awsInstall/enterprise-backup/devops/infra/envs/aws") {
            sh """sed -i '/^  vault:/!b;n;s/^\\(    \\)enabled:\\s*false/\\1enabled: true/' common_vars.yaml"""
        }
    }
}

return this
def backupBranch = [string(name: 'cli-backup-branch', value: params.'cli-backup-branch')]

def cliBackup = [string(name: 'test-tags', value: 'CLI-BACKUP')]
def cliBackupSequential = [string(name: 'test-tags', value: 'CLI-BACKUP-SEQUENTIAL')]
def cliBackupNegative = [string(name: 'test-tags', value: 'CLI-BACKUP-NEGATIVE')]
def cliBackupCtoGo = [string(name: 'tests-tag', value: 'CLI-BACKUP-C-TO-GO')]
def cliBackupGoToC = [string(name: 'tests-tag', value: 'CLI-BACKUP-GO-TO-C')]
def cliBackupTLS = [string(name: 'tests-tag', value: 'BACKUP-TLS-ENV'), booleanParam(name: 'asdb-tls-enabled', value: true)]
def cliBackup3Nodes = [string(name: 'tests-tag', value: 'CLI-3-NODES-CLUSTER')]

pipeline {
    agent none

    triggers {
        cron('0 2 * * 0-4') // every Sunday to Thursday at 6am IST (2am UTC)
    }

    parameters {
        booleanParam(name: 'run_test_cli_backup', defaultValue: true)
        booleanParam(name: 'run_test_cli_backup_sequential', defaultValue: true)
        booleanParam(name: 'run_test_cli_backup_c_to_go', defaultValue: true)
        booleanParam(name: 'run_test_cli_backup_go_to_c', defaultValue: true)
        booleanParam(name: 'run_test_cli_backup_negative', defaultValue: true)
        booleanParam(name: 'run_test_cli_backup_tls', defaultValue: true)
        booleanParam(name: 'run_test_cli_backup_3_nodes_cluster_tests', defaultValue: true)

        string(name: 'automation_branch', defaultValue: 'master', description: 'The Automation branch with the tests you want to run')
        string(name: 'cli-backup-branch', defaultValue: 'main', description: 'The CLI Backup branch you want to install from')
    }

    options {
        timeout(time: 60, unit: 'MINUTES')
        overrideIndexTriggers(false)
    }

    stages {
        stage('Run CLI backup tests') {
            parallel {
                stage('Run test_cli_backup') {
                    when {
                        expression { return params.run_test_cli_backup }
                    }
                    steps {
                        build job: "cli/multi-node-cli-local-env", wait: false, propagate: true
                        sleep 20
                        build job: "cli/multi-node-cli-local-env/${automation_branch}", parameters: backupBranch + cliBackup, propagate: true
                    }
                }
                stage('Run test_cli_backup_sequential') {
                    when {
                        expression { return params.run_test_cli_backup_sequential }
                    }
                    steps {
                        build job: "cli/multi-node-cli-local-env", wait: false, propagate: true
                        sleep 20
                        build job: "cli/multi-node-cli-local-env/${automation_branch}", parameters: backupBranch + cliBackupSequential, propagate: true
                    }
                }
                stage('Run cli/test-cli-tools-locally-c-to-go') {
                    when {
                        expression { return params.run_test_cli_backup_c_to_go }
                    }
                    steps {
                        build job: "cli/test-cli-tools-locally", wait: false, propagate: true
                        sleep 20
                        build job: "cli/test-cli-tools-locally/${automation_branch}", parameters: backupBranch + cliBackupCtoGo, propagate: true
                    }
                }
                stage('Run cli/test-cli-tools-locally-go-to-c') {
                    when {
                        expression { return params.run_test_cli_backup_go_to_c }
                    }
                    steps {
                        build job: "cli/test-cli-tools-locally", wait: false, propagate: true
                        sleep 20
                        build job: "cli/test-cli-tools-locally/${automation_branch}", parameters: backupBranch + cliBackupGoToC, propagate: true
                    }
                }
                stage('Run test_cli_backup_negative') {
                    when {
                        expression { return params.run_test_cli_backup_negative }
                    }
                    steps {
                        build job: "cli/multi-node-cli-local-env", wait: false, propagate: true
                        sleep 20
                        build job: "cli/multi-node-cli-local-env/${automation_branch}", parameters: backupBranch + cliBackupNegative, propagate: true
                    }
                }
                stage('Run run_test_cli_backup_tls') {
                    when {
                        expression { return params.run_test_cli_backup_tls }
                    }
                    steps {
                        build job: "cli/multi-node-cli-local-env", wait: false, propagate: true
                        sleep 20
                        build job: "cli/multi-node-cli-local-env/${automation_branch}", parameters: backupBranch + cliBackupTLS, propagate: true
                    }
                }
                stage('Run run_test_cli_backup_3_nodes_cluster_tests') {
                    when {
                        expression { return params.run_test_cli_backup_3_nodes_cluster_tests }
                    }
                    steps {
                        build job: "cli/multi-node-cli-local-env", wait: false, propagate: true
                        sleep 20
                        build job: "cli/multi-node-cli-local-env/${automation_branch}", parameters: backupBranch + cliBackup3Nodes, propagate: true
                    }
                }
            }
        }
    }
}
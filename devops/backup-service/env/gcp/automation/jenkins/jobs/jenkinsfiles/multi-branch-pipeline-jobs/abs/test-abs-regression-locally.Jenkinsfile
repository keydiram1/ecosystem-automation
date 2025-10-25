def absBranch = [string(name: 'abs-branch', value: params.'abs_branch')]
def aersopikeClusterV6 = [string(name: 'asdb-version', value: '6.3.0.31')]
def installationSource = [string(name: 'abs-version', value: params.'abs_version'),
booleanParam(name: 'pull-from-jfrog', value: params.'pull_from_jfrog')]
def absE2e = [string(name: 'tests-tag', value: 'ABS-E2E')]
def sequentialTests = [string(name: 'tests-tag', value: 'ABS-SEQUENTIAL-TESTS')]
def sequentialTests2 = [string(name: 'tests-tag', value: 'ABS-SEQUENTIAL-TESTS-2')]
def negativeTests = [string(name: 'tests-tag', value: 'ABS-NEGATIVE-TESTS')]
def serviceTest = [string(name: 'tests-tag', value: 'ABS-SERVICE-TEST')]
def localLoadTest = [string(name: 'tests-tag', value: 'ABS-LOCAL-LOAD-TEST')]
def cToGo = [string(name: 'tests-tag', value: 'ABS-C-TO-GO')]
def goToC = [string(name: 'tests-tag', value: 'ABS-GO-TO-C')]
def gcp = [string(name: 'storage-provider', value: 'gcp')]
def azure = [string(name: 'storage-provider', value: 'azure')]
def aws = [string(name: 'storage-provider', value: 'aws')]
def s3ConfigFile = [string(name: 'tests-tag', value: 'ABS-CONFIGURATIONS'),
                  string(name: 'abs-configuration-file', value: 's3')]
def httpConfigFile = [string(name: 'tests-tag', value: 'ABS-CONFIGURATIONS'),
                string(name: 'abs-configuration-file', value: 'http')]

pipeline {
    agent none

    parameters {
        booleanParam(name: 'run_test_abs_local_v8', defaultValue: true)
        booleanParam(name: 'run_test_abs_negative_local', defaultValue: true)
        booleanParam(name: 'run_test_abs_sequential_local', defaultValue: true)
        booleanParam(name: 'run_test_abs_sequential_local_2', defaultValue: true)
        booleanParam(name: 'run_test_abs_configurations_local', defaultValue: true)
        booleanParam(name: 'run_test_abs_service_local', defaultValue: true)
        booleanParam(name: 'run_test_abs_local_v6', defaultValue: true)
        booleanParam(name: 'run_test_abs_c_to_go_local', defaultValue: true)
        booleanParam(name: 'run_test_abs_s3_config_file', defaultValue: true)
        booleanParam(name: 'run_test_abs_http_config_file', defaultValue: true)
        booleanParam(name: 'run_test_abs_sequential_gcp', defaultValue: false)
        booleanParam(name: 'run_test_abs_sequential_azure', defaultValue: false)
        booleanParam(name: 'run_test_abs_sequential_aws', defaultValue: false)
        booleanParam(name: 'run_abs_local_load_test', defaultValue: false)

        string(name: 'automation_branch', defaultValue: 'master', description: 'The Automation branch with the tests you want to run')
        string(name: 'abs_branch', defaultValue: 'v3', description: 'The ABS branch you want to install from')

        booleanParam(name: 'pull_from_jfrog', defaultValue: false)
        string(name: 'abs_version', defaultValue: 'tests', description: 'Replace the tag of the Jfrog-ABS images')
    }

    environment {
        GITHUB_TOKEN = credentials("github-token")
    }

    options {
        timeout(time: 60, unit: 'MINUTES')
        overrideIndexTriggers(false)
    }

    triggers {
        cron('0 2 * * 0-4') // every Sunday to Thursday at 5am IST (3am UTC)
    }

    stages {
        stage('Run ABS tests') {
            parallel {
                stage('Run test-abs-locally AS V7') {
                    when {
                        expression { return params.run_test_abs_local_v8 }
                    }
                    steps {
                        build job: "abs/test-abs-locally", wait: false, propagate: true
                        sleep 20
                        build job: "abs/test-abs-locally/${automation_branch}", parameters: absBranch + installationSource + absE2e, propagate: true
                    }
                }
                stage('Run test-abs-negative-local') {
                    when {
                        expression { return params.run_test_abs_negative_local }
                    }
                    steps {
                        build job: "abs/test-abs-locally", wait: false, propagate: true
                        sleep 60
                        build job: "abs/test-abs-locally/${automation_branch}", parameters: absBranch + installationSource + negativeTests, propagate: true
                    }
                }
                stage('Run test-abs-sequential-local') {
                    when {
                        expression { return params.run_test_abs_sequential_local }
                    }
                    steps {
                        build job: "abs/test-abs-locally", wait: false, propagate: true
                        sleep 20
                        build job: "abs/test-abs-locally/${automation_branch}", parameters: absBranch + installationSource + sequentialTests, propagate: true
                    }
                }
                stage('Run test-abs-sequential-local-2') {
                    when {
                        expression { return params.run_test_abs_sequential_local_2 }
                    }
                    steps {
                        build job: "abs/test-abs-locally", wait: false, propagate: true
                        sleep 20
                        build job: "abs/test-abs-locally/${automation_branch}", parameters: absBranch + installationSource + sequentialTests2, propagate: true
                    }
                }
//                stage('Run test-abs-configurations-local') {
//                    when {
//                        expression { return params.run_test_abs_configurations_local }
//                    }
//                    steps {
//                        build job: "test-abs-configurations-local", wait: false, propagate: true
//                        sleep 20
//                        build job: "test-abs-configurations-local/${automation_branch}", parameters: absBranch + installationSource, propagate: true
//                    }
//                }
                stage('Run test-abs-service-local') {
                    when {
                        expression { return params.run_test_abs_service_local }
                    }
                    steps {
                        build job: "abs/test-abs-locally", wait: false, propagate: true
                        sleep 60
                        build job: "abs/test-abs-locally/${automation_branch}", parameters: absBranch + installationSource + serviceTest, propagate: true
                    }
                }
                stage('Run test-abs-locally AS V6') {
                    when {
                        expression { return params.run_test_abs_local_v6 }
                    }
                    steps {
                        build job: "abs/test-abs-locally", wait: false, propagate: true
                        sleep 20
                        build job: "abs/test-abs-locally/${automation_branch}", parameters: absBranch + aersopikeClusterV6 + installationSource + absE2e, propagate: true
                    }
                }
                stage('Run test-abs-c-to-go-local') {
                    when {
                        expression { return params.run_test_abs_c_to_go_local }
                    }
                    steps {
                        build job: "abs/test-abs-locally", wait: false, propagate: true
                        sleep 60
                        build job: "abs/test-abs-locally/${automation_branch}", parameters: absBranch + installationSource + cToGo, propagate: true
                    }
                }
                stage('Run test-abs-go-to-c-local') {
                    when {
                        expression { return params.run_test_abs_c_to_go_local }
                    }
                    steps {
                        build job: "abs/test-abs-locally", wait: false, propagate: true
                        sleep 60
                        build job: "abs/test-abs-locally/${automation_branch}", parameters: absBranch + installationSource + goToC, propagate: true
                    }
                }
                stage('Run test-abs-locally with S3 config file') {
                    when {
                        expression { return params.run_test_abs_s3_config_file }
                    }
                    steps {
                        build job: "abs/test-abs-locally", wait: false, propagate: true
                        sleep 20
                        build job: "abs/test-abs-locally/${automation_branch}", parameters: absBranch + installationSource + s3ConfigFile, propagate: true
                    }
                }
                stage('Run test-abs-locally with http config file') {
                    when {
                        expression { return params.run_test_abs_http_config_file }
                    }
                    steps {
                        build job: "abs/test-abs-locally", wait: false, propagate: true
                        sleep 20
                        build job: "abs/test-abs-locally/${automation_branch}", parameters: absBranch + installationSource + httpConfigFile, propagate: true
                    }
                }
                stage('Run Sequential GCP Tests') {
                    when {
                        expression { return params.run_test_abs_sequential_gcp }
                    }
                    steps {
                        build job: "abs/test-abs-locally", wait: false, propagate: true
                        sleep 20
                        build job: "abs/test-abs-locally/${automation_branch}", parameters: absBranch + installationSource + sequentialTests + gcp, propagate: true

                        build job: "abs/test-abs-locally", wait: false, propagate: true
                        sleep 20
                        build job: "abs/test-abs-locally/${automation_branch}", parameters: absBranch + installationSource + sequentialTests2 + gcp, propagate: true
                    }
                }
                stage('Run Sequential Azure Tests') {
                    when {
                        expression { return params.run_test_abs_sequential_azure }
                    }
                    steps {
                        build job: "abs/test-abs-locally", wait: false, propagate: true
                        sleep 20
                        build job: "abs/test-abs-locally/${automation_branch}", parameters: absBranch + installationSource + sequentialTests + azure, propagate: true

                        build job: "abs/test-abs-locally", wait: false, propagate: true
                        sleep 20
                        build job: "abs/test-abs-locally/${automation_branch}", parameters: absBranch + installationSource + sequentialTests2 + azure, propagate: true
                    }
                }
                stage('Run Sequential AWS Tests') {
                    when {
                        expression { return params.run_test_abs_sequential_aws }
                    }
                    steps {
                        build job: "abs/test-abs-locally", wait: false, propagate: true
                        sleep 20
                        build job: "abs/test-abs-locally/${automation_branch}", parameters: absBranch + installationSource + sequentialTests + aws, propagate: true

                        build job: "abs/test-abs-locally", wait: false, propagate: true
                        sleep 20
                        build job: "abs/test-abs-locally/${automation_branch}", parameters: absBranch + installationSource + sequentialTests2 + aws, propagate: true
                    }
                }
                stage('Run The local load test') {
                    when {
                        expression { return params.run_abs_local_load_test }
                    }
                    steps {
                        build job: "abs/test-abs-locally", wait: false, propagate: true
                        sleep 20
                        build job: "abs/test-abs-locally/${automation_branch}", parameters: absBranch + installationSource + localLoadTest, propagate: true
                    }
                }
            }
        }
    }
}
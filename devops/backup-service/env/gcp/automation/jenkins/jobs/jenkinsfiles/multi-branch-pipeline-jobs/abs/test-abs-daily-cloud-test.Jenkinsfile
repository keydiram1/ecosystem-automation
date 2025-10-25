def commonParams = [
        string(name: 'test-tags', value: 'ABS-E2E,ABS-NEGATIVE-TESTS,ABS-SEQUENTIAL-TESTS,ABS-SEQUENTIAL-TESTS-2'),
        string(name: 'ws', value: params.ws)
]

pipeline {
    agent none

    triggers {
        cron('0 3 * * 0-4') // every Sunday to Thursday at 6am IST (3am UTC)
    }

    parameters {
        choice(name: 'ws', choices: ['ws-2', 'ws-1', 'ws-3'], description: 'Workspace to use')
        string(name: 'automation_branch', defaultValue: 'master')
        string(name: 'abs-branch', defaultValue: 'v3', description: 'Set the abs branch to create image from')
        string(name: 'abs-image-tag', defaultValue: 'v3', description: 'Set abs image tag')

        booleanParam(name: 'run_build_abs_image', defaultValue: true)
        booleanParam(name: 'run_deploy_asdb', defaultValue: true)
        booleanParam(name: 'run_deploy_abs', defaultValue: true)

        booleanParam(name: 'run_functional_cloud_tests', defaultValue: true)

        booleanParam(name: 'run_destroy_abs', defaultValue: true)
        booleanParam(name: 'run_destroy_asdb', defaultValue: true)
    }

    options {
        timeout(time: 600, unit: 'MINUTES')
    }

    stages {
        stage('Build ABS Image') {
            when {
                expression { return params.run_build_abs_image }
            }
            steps {
                build job: "devops/build-artifacts", parameters: [
                        string(name: 'image-repository', value: 'remote'),
                        string(name: 'tag', value: params.'abs-image-tag'),
                        string(name: 'branch', value: params.'abs-branch'),
                        string(name: 'repository', value: 'https://github.com/aerospike/aerospike-backup-service.git'),
                        string(name: 'artifacts', value: 'true,false,false,false'),
                        string(name: 'architecture', value: 'true,true'),
                        string(name: 'jfrog-repo', value: 'dev')
                ]
            }
        }

        stage('Deploy ASDB') {
            when { expression { return params.run_deploy_asdb } }
            steps {
                build job: "devops/deploy-asdb/${params.automation_branch}", parameters: [
                        string(name: 'ws', value: params.ws),
                        string(name: 'abs-image-tag', value: params.ws),
                        string(name: 'asdb-size', value: '3'),
                        string(name: 'asdb-machine-type', value: 'n2-standard-8'),

                        string(name: 'asdb-version', value: '8.0.0.8'),
                        string(name: 'asdb-distro', value: 'ubuntu24.04'),
                        string(name: 'asdb-archs', value: 'amd64'),
                        string(name: 'asdb-section-selection', value: 'true,false,false'),
                        booleanParam(name: 'asdb-device-shadow', value: false),
                        booleanParam(name: 'multi-zone-deployment', value: false),
                        booleanParam(name: 'asdb-sc-enabled', value: false),
                        booleanParam(name: 'ops-agent', value: false),
                        booleanParam(name: 'secret-agent', value: true),
                        booleanParam(name: 'debug-mode', value: false),
                        string(name: 'asdb-security-type', value: 'TLS'),
                        string(name: 'asdb-single-query-threads', value: '12')
                ]
            }
        }

        stage('Deploy ABS') {
            when { expression { return params.run_deploy_abs } }
            steps {
                build job: "devops/deploy-abs/${params.automation_branch}", parameters: [
                        string(name: 'ws', value: params.ws),
                        string(name: 'abs-image-tag', value: params.'abs-image-tag')
                ]
            }
        }

        stage('ABS Cloud Tests') {
            when { expression { return params.run_functional_cloud_tests } }
            steps {
                build job: "abs/test-abs-gcp/${params.automation_branch}", parameters: commonParams , propagate: false
            }
        }

        stage('Destroy ABS') {
            when { expression { return params.run_destroy_abs } }
            steps {
                build job: "devops/destroy-abs/${params.automation_branch}", parameters: [
                        string(name: 'ws', value: params.ws)
                ]
            }
        }

        stage('Destroy ASDB') {
            when { expression { return params.run_destroy_asdb } }
            steps {
                build job: "devops/destroy-asdb/${params.automation_branch}", parameters: [
                        string(name: 'ws', value: params.ws)
                ]
            }
        }
    }
}
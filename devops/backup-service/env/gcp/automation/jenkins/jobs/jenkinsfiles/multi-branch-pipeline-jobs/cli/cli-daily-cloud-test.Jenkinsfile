def commonParams = [
        string(name: 'test-tags', value: 'CLI-BACKUP,CLI-BACKUP-SEQUENTIAL,CLI-BACKUP-NEGATIVE,CLI-PERFORMANCE-TEST'),
        string(name: 'ws', value: params.ws)
]

pipeline {
    agent none

//    triggers {
//        cron('0 3 * * 0-4') // every Sunday to Thursday at 6am IST (3am UTC)
//    }

    parameters {
        choice(name: 'ws', choices: ['ws-2', 'ws-1', 'ws-3'], description: 'Workspace to use')
        string(name: 'automation_branch', defaultValue: 'master')

        booleanParam(name: 'run_deploy_asdb', defaultValue: true)

        booleanParam(name: 'run_functional_cloud_tests', defaultValue: true)

        booleanParam(name: 'run_destroy_asdb', defaultValue: true)
    }

    options {
        timeout(time: 600, unit: 'MINUTES')
    }

    stages {
        stage('Deploy ASDB') {
            when { expression { return params.run_deploy_asdb } }
            steps {
                build job: "devops/deploy-asdb/${params.automation_branch}", parameters: [
                        string(name: 'ws', value: params.ws),
                        string(name: 'asdb-size', value: '3'),
                        string(name: 'asdb-machine-type', value: 'n2-standard-8'),

                        string(name: 'asdb-version', value: '8.0.0.8'),
                        string(name: 'asdb-distro', value: 'ubuntu24.04'),
                        string(name: 'asdb-archs', value: 'amd64'),
                        string(name: 'asdb-section-selection', value: 'true,false,false'),
                        booleanParam(name: 'asdb-device-shadow', value: false),
                        booleanParam(name: 'asdb-sc-enabled', value: false),
                        booleanParam(name: 'ops-agent', value: false),
                        booleanParam(name: 'secret-agent', value: true),
                        booleanParam(name: 'debug-mode', value: false),
                        string(name: 'asdb-security-type', value: 'TLS')
                ]
            }
        }

        stage('CLI Cloud Tests') {
            when { expression { return params.run_functional_cloud_tests } }
            steps {
                build job: "cli/test-cli-gcp/${params.automation_branch}", parameters: commonParams , propagate: false
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
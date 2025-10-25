def commonParams = [
        string(name: 'create-data', value: 'y'),
        string(name: 'truncate-data', value: 'y'),
        string(name: 'test-tags', value: 'CLI-PERFORMANCE-TEST'),
        string(name: 'ws', value: params.ws)
]

pipeline {
    agent none

//    triggers {
//        cron('0 19 * * 6') // every Saturday at 22:00 IST (19:00 UTC during summer)
//    }

    parameters {
        choice(name: 'ws', choices: ['ws-3', 'ws-2', 'ws-1'], description: 'Workspace to use')
        string(name: 'automation_branch', defaultValue: 'master')
        string(name: 'cli-branch', defaultValue: 'main', description: 'Set the cli branch to create image from')

        booleanParam(name: 'run_deploy_asdb', defaultValue: true)
        booleanParam(name: 'run_destroy_asdb', defaultValue: true)

        booleanParam(name: 'run_performance_scalar_1kb', defaultValue: true)
        booleanParam(name: 'run_performance_complex_1kb', defaultValue: true)
        booleanParam(name: 'run_performance_mixed_1kb', defaultValue: true)
        booleanParam(name: 'run_performance_scalar_3kb', defaultValue: true)
        booleanParam(name: 'run_performance_complex_3kb', defaultValue: true)
        booleanParam(name: 'run_performance_mixed_3kb', defaultValue: true)
        booleanParam(name: 'run_performance_scalar_100kb', defaultValue: true)
        booleanParam(name: 'run_performance_complex_100kb', defaultValue: true)
        booleanParam(name: 'run_performance_mixed_100kb', defaultValue: true)
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
                        string(name: 'asdb-size', value: '5'),
                        string(name: 'asdb-machine-type', value: 'n2-standard-16'),
                        string(name: 'asdb-device-type', value: 'raw'),
                        string(name: 'asdb-devices', value: '4'),
                        string(name: 'asdb-namespaces', value: '1'),

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

        stage('Run CLI Performance Tests') {
            stages {
                stage('SCALAR_1KB') {
                    when { expression { return params.run_performance_scalar_1kb } }
                    steps {
                        build job: "cli/test-cli-gcp/${params.automation_branch}", parameters: commonParams + [
                                string(name: 'data-type', value: 'SCALAR_1KB'),
                                string(name: 'number-of-records-in-millions', value: '30')
                        ], propagate: false
                    }
                }

                stage('COMPLEX_1KB') {
                    when { expression { return params.run_performance_complex_1kb } }
                    steps {
                        build job: "cli/test-cli-gcp/${params.automation_branch}", parameters: commonParams + [
                                string(name: 'data-type', value: 'COMPLEX_1KB'),
                                string(name: 'number-of-records-in-millions', value: '50')
                        ], propagate: false
                    }
                }

                stage('MIXED_1KB') {
                    when { expression { return params.run_performance_mixed_1kb } }
                    steps {
                        build job: "cli/test-cli-gcp/${params.automation_branch}", parameters: commonParams + [
                                string(name: 'data-type', value: 'MIXED_1KB'),
                                string(name: 'number-of-records-in-millions', value: '50')
                        ], propagate: false
                    }
                }

                stage('SCALAR_3KB') {
                    when { expression { return params.run_performance_scalar_3kb } }
                    steps {
                        build job: "cli/test-cli-gcp/${params.automation_branch}", parameters: commonParams + [
                                string(name: 'data-type', value: 'SCALAR_3KB'),
                                string(name: 'number-of-records-in-millions', value: '50')
                        ], propagate: false
                    }
                }

                stage('COMPLEX_3KB') {
                    when { expression { return params.run_performance_complex_3kb } }
                    steps {
                        build job: "cli/test-cli-gcp/${params.automation_branch}", parameters: commonParams + [
                                string(name: 'data-type', value: 'COMPLEX_3KB'),
                                string(name: 'number-of-records-in-millions', value: '20')
                        ], propagate: false
                    }
                }

                stage('MIXED_3KB') {
                    when { expression { return params.run_performance_mixed_3kb } }
                    steps {
                        build job: "cli/test-cli-gcp/${params.automation_branch}", parameters: commonParams + [
                                string(name: 'data-type', value: 'MIXED_3KB'),
                                string(name: 'number-of-records-in-millions', value: '25')
                        ], propagate: false
                    }
                }

                stage('SCALAR_100KB') {
                    when { expression { return params.run_performance_scalar_100kb } }
                    steps {
                        build job: "cli/test-cli-gcp/${params.automation_branch}", parameters: commonParams + [
                                string(name: 'data-type', value: 'SCALAR_100KB'),
                                string(name: 'number-of-records-in-millions', value: '3')
                        ], propagate: false
                    }
                }

                stage('COMPLEX_100KB') {
                    when { expression { return params.run_performance_complex_100kb } }
                    steps {
                        build job: "cli/test-cli-gcp/${params.automation_branch}", parameters: commonParams + [
                                string(name: 'data-type', value: 'COMPLEX_100KB'),
                                string(name: 'number-of-records-in-millions', value: '1')
                        ], propagate: false
                    }
                }

                stage('MIXED_100KB') {
                    when { expression { return params.run_performance_mixed_100kb } }
                    steps {
                        build job: "cli/test-cli-gcp/${params.automation_branch}", parameters: commonParams + [
                                string(name: 'data-type', value: 'MIXED_100KB'),
                                string(name: 'number-of-records-in-millions', value: '1')
                        ], propagate: false
                    }
                }
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
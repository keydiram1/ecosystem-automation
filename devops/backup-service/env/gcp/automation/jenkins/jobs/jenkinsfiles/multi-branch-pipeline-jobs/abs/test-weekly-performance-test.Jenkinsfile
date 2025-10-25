def commonParams = [
        string(name: 'create-data', value: 'y'),
        string(name: 'truncate-data', value: 'y'),
        string(name: 'test-tags', value: 'ABS-PERFORMANCE-TEST'),
        string(name: 'ws', value: params.ws)
]

pipeline {
    agent none

    triggers {
        cron('0 19 * * 6') // every Saturday at 22:00 IST (19:00 UTC during summer)
    }

    parameters {
        choice(name: 'ws', choices: ['ws-3', 'ws-2', 'ws-1'], description: 'Workspace to use')
        string(name: 'automation_branch', defaultValue: 'master')
        string(name: 'abs-branch', defaultValue: 'v3', description: 'Set the abs branch to create image from')
        string(name: 'abs-image-tag', defaultValue: 'v3', description: 'Set abs image tag')

        booleanParam(name: 'run_build_abs_image', defaultValue: true)
        booleanParam(name: 'run_deploy_asdb', defaultValue: true)
        booleanParam(name: 'run_deploy_abs', defaultValue: true)
        booleanParam(name: 'run_destroy_abs', defaultValue: true)
        booleanParam(name: 'run_destroy_asdb', defaultValue: true)

        booleanParam(name: 'run_local_load_test', defaultValue: true)

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
                        string(name: 'asdb-size', value: '5'),
                        string(name: 'asdb-machine-type', value: 'n2-standard-16'),
                        string(name: 'asdb-device-type', value: 'raw'),
                        string(name: 'asdb-devices', value: '4'),
                        string(name: 'asdb-namespaces', value: '1'),
                        string(name: 'asdb-single-query-threads', value: '12'),

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

        stage('Deploy ABS') {
            when { expression { return params.run_deploy_abs } }
            steps {
                build job: "devops/deploy-abs/${params.automation_branch}", parameters: [
                        string(name: 'ws', value: params.ws),
                        string(name: 'abs-image-tag', value: params.'abs-image-tag'),
                        string(name: 'asdb-device-type', value: 'raw')
                ]
            }
        }

        stage('Run ABS Performance Tests') {
            stages {
                stage('SCALAR_1KB') {
                    when { expression { return params.run_performance_scalar_1kb } }
                    steps {
                        build job: "abs/test-abs-gcp/${params.automation_branch}", parameters: commonParams + [
                                string(name: 'data-type', value: 'SCALAR_1KB'),
                                string(name: 'number-of-records-in-millions', value: '30')
                        ], propagate: false
                    }
                }

                stage('COMPLEX_1KB') {
                    when { expression { return params.run_performance_complex_1kb } }
                    steps {
                        build job: "abs/test-abs-gcp/${params.automation_branch}", parameters: commonParams + [
                                string(name: 'data-type', value: 'COMPLEX_1KB'),
                                string(name: 'number-of-records-in-millions', value: '50')
                        ], propagate: false
                    }
                }

                stage('MIXED_1KB') {
                    when { expression { return params.run_performance_mixed_1kb } }
                    steps {
                        build job: "abs/test-abs-gcp/${params.automation_branch}", parameters: commonParams + [
                                string(name: 'data-type', value: 'MIXED_1KB'),
                                string(name: 'number-of-records-in-millions', value: '50')
                        ], propagate: false
                    }
                }

                stage('SCALAR_3KB') {
                    when { expression { return params.run_performance_scalar_3kb } }
                    steps {
                        build job: "abs/test-abs-gcp/${params.automation_branch}", parameters: commonParams + [
                                string(name: 'data-type', value: 'SCALAR_3KB'),
                                string(name: 'number-of-records-in-millions', value: '50')
                        ], propagate: false
                    }
                }

                stage('COMPLEX_3KB') {
                    when { expression { return params.run_performance_complex_3kb } }
                    steps {
                        build job: "abs/test-abs-gcp/${params.automation_branch}", parameters: commonParams + [
                                string(name: 'data-type', value: 'COMPLEX_3KB'),
                                string(name: 'number-of-records-in-millions', value: '20')
                        ], propagate: false
                    }
                }

                stage('MIXED_3KB') {
                    when { expression { return params.run_performance_mixed_3kb } }
                    steps {
                        build job: "abs/test-abs-gcp/${params.automation_branch}", parameters: commonParams + [
                                string(name: 'data-type', value: 'MIXED_3KB'),
                                string(name: 'number-of-records-in-millions', value: '25')
                        ], propagate: false
                    }
                }

                stage('SCALAR_100KB') {
                    when { expression { return params.run_performance_scalar_100kb } }
                    steps {
                        build job: "abs/test-abs-gcp/${params.automation_branch}", parameters: commonParams + [
                                string(name: 'data-type', value: 'SCALAR_100KB'),
                                string(name: 'number-of-records-in-millions', value: '3')
                        ], propagate: false
                    }
                }

                stage('COMPLEX_100KB') {
                    when { expression { return params.run_performance_complex_100kb } }
                    steps {
                        build job: "abs/test-abs-gcp/${params.automation_branch}", parameters: commonParams + [
                                string(name: 'data-type', value: 'COMPLEX_100KB'),
                                string(name: 'number-of-records-in-millions', value: '1')
                        ], propagate: false
                    }
                }

                stage('MIXED_100KB') {
                    when { expression { return params.run_performance_mixed_100kb } }
                    steps {
                        build job: "abs/test-abs-gcp/${params.automation_branch}", parameters: commonParams + [
                                string(name: 'data-type', value: 'MIXED_100KB'),
                                string(name: 'number-of-records-in-millions', value: '1')
                        ], propagate: false
                    }
                }
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

        stage('Run Local Load Test') {
            when { expression { return params.run_local_load_test } }
            steps {
                build job: "abs/test-abs-locally/${params.automation_branch}", parameters: [
                        string(name: 'tests-tag', value: 'ABS-LOCAL-LOAD-TEST'),
                        string(name: 'abs-branch', value: params.'abs-branch')
                ], propagate: false
            }
        }
    }
}

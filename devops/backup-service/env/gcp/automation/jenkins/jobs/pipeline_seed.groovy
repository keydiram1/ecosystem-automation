#!/usr/bin/env groovy

String repoUrl = "https://github.com/citrusleaf/ecosystem-automation"
String credId = "github-token"
int keep = 20

def createPipelineJob(String repoUrl, String credId, int keep, String jenkinsfile) {

    String filename = jenkinsfile.tokenize('/').last().replace('.Jenkinsfile', '')
    String basename = jenkinsfile.tokenize('/').dropRight(1).last()

    return pipelineJob("${basename}/${filename}") {
        displayName(filename)

        definition {
            cpsScm {
                scm {
                    git {
                        remote {
                            url(repoUrl)
                            credentials(credId)
                        }
                        branch("*/master")
                        extensions {
                            cleanBeforeCheckout()
                        }
                    }
                }
                scriptPath(jenkinsfile)
            }
        }

        logRotator {
            daysToKeep(keep)
        }

        authorization {
            permissionAll("anonymous")
        }
    }
}

jenkinsfiles.each { jenkinsfile ->
    println("Creating pipeline job for ${jenkinsfile.path}")
    createPipelineJob(repoUrl, credId, keep, jenkinsfile.path)
}

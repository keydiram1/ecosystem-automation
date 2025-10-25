#!/usr/bin/env groovy
String owner = "citrusleaf"
String repo = "ecosystem-automation"
String url = "https://github.com/citrusleaf/ecosystem-automation"
String credId = "github-token"
int keep = 20

def generateUUIDForString(String name) {
    return UUID.nameUUIDFromBytes(name.getBytes()).toString();
}

def createMultibranchPipelineJobs(String owner, String repo, String url, String credId, int keep, String jenkinsfile){

    String filename = jenkinsfile.tokenize('/').last().replace('.Jenkinsfile', '')
    String basename = jenkinsfile.tokenize('/').dropRight(1).last()

    return multibranchPipelineJob("${basename}/${filename}") {
        displayName(filename)

        authorization {
            permissionAll("anonymous")
        }

        orphanedItemStrategy {
            discardOldItems {
                daysToKeep(keep)
            }
        }
        triggers {
            periodicFolderTrigger {
                interval("1h")
            }
        }
        factory {
            workflowBranchProjectFactory {
                scriptPath(jenkinsfile)
            }
        }
        branchSources {
            branchSource {
                source {
                    github {
                        repoOwner(owner)
                        repository(repo)
                        configuredByUrl(true)
                        repositoryUrl(url)
                        credentialsId(credId)
                        id(this.generateUUIDForString(jenkinsfile))
                        traits {
                            cloneOption {
                                extension {
                                    honorRefspec(true)
                                    noTags(false)
                                    timeout(3)
                                    shallow(false)
                                    reference("")
                                }
                            }
                            cleanAfterCheckout {
                                extension {
                                    deleteUntrackedNestedRepositories(true)
                                }
                            }
                            gitHubBranchDiscovery {
                                // 1 Exclude branches that are also filed as PRs
                                // 2 Only branches that are also filed as PRs
                                // 3 All branches
                                strategyId(3)
                            }
                            gitHubPullRequestDiscovery {
                                // 1 Merging the pull request with the current target branch revision
                                // 2 The current pull request revision
                                // 3 Both the current pull request revision and the pull request merged with the current target branch revision
                                strategyId(1)
                            }
                            gitHubTagDiscovery()
                        }
                    }
                }
                // https://stackoverflow.com/questions/55173365/how-to-disable-triggers-from-branch-indexing-but-still-allow-scm-triggering-in-m
                strategy {
                    allBranchesSame {
                        props {
                            suppressAutomaticTriggering {
                                strategy('INDEXING')
                            }
                        }
                    }
                }
            }
        }
    }
}

jenkinsfiles.each { jenkinsfile ->
    println("Creating multi-branch pipeline job for $jenkinsfile.path")
    createMultibranchPipelineJobs(owner, repo, url, credId, keep, jenkinsfile.path)
}

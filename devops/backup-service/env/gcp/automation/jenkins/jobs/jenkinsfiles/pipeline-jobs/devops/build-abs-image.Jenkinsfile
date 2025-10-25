pipeline {
    agent {
        label "jenkins-gcp-env-provisioner"
    }

    parameters {

        string(
            name: "abs-branch",
            defaultValue: "v3",
            description: "Enter the branch name to use for the build process. Defaults to 'v3'."
        )

        choice(
            name: "image-repository",
            choices: ["local", "remote"],
            description: "Choose the source of the image repository: 'local' for locally hosted repositories or 'remote' for external repositories."
        )

        string(
            name: "image-tag",
            defaultValue: "v3",
            description: "Enter the tag to assign to the built image. Defaults to 'latest'."
        )

        choice(
            name: "latest",
            choices: ["false", "true"],
            description: "Specify whether to tag the built image as 'latest'. Choose 'true' to apply the 'latest' tag or 'false' to skip it."
        )

    }

    options {
        timeout(time: 1, unit: "HOURS")
        buildDiscarder(logRotator(numToKeepStr: "20", daysToKeepStr: "30"))
        overrideIndexTriggers(false)
    }

    environment {
        ABS_BRANCH = "${params["abs-branch"]}"
        IMAGE_REPOSITORY = "${params["image-repository"]}"
        IMAGE_TAG = "${params["image-tag"]}"
        LATEST = "${params["latest"]}"
        REGISTRY = "aerospike.jfrog.io/ecosystem-dockerhub-mirror"
    }

    stages {
        stage("Checkout") {
            steps {
                git branch: env.ABS_BRANCH,
                changelog: false,
                poll: false,
                url: "https://github.com/aerospike/aerospike-backup-service"
            }
        }
        stage("Build ABS image for local image repository") {
            when {
                environment name: "IMAGE_REPOSITORY", value: "local"
            }
            steps {
                sh "make docker-build"
            }
        }
        stage("Build ABS image for remote image repository") {
            when {
                environment name: "IMAGE_REPOSITORY", value: "remote"
            }
            steps {
                withCredentials([
                    string(credentialsId: "docker-username", variable: "DOCKER_USERNAME"),
                    string(credentialsId: "docker-password", variable: "DOCKER_PASSWORD")]) {

                      sh "docker buildx create --name builder --driver docker-container --use"
                      sh "docker buildx inspect --bootstrap"
                      sh "make docker-buildx"
                      sh "docker context use default"
                      sh "docker buildx rm builder"
                }
            }
        }
    }
}

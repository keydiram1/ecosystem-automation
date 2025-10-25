group default {
    targets = [
        "abs-base-image-cache",
        "aerospike-backup-service"
    ]
}

variable LATEST {
    default = false
}

variable TAG {
    default = ""
}

variable GIT_BRANCH {
    default = null
}

variable GIT_COMMIT_SHA {
    default = null
}

variable VERSION {
    default = null
}

variable ISO8601 {
    default = null
}

variable CACHEBUST {
    default = "1"
}

variable HUB {
    default = "aerospike.jfrog.io/ecosystem-container-dev-local"
}

variable PLATFORMS {
    default = "linux/amd64,linux/arm64"
}

function tags {
    params = [service]
    result = LATEST == true ? [
        "${HUB}/${service}:${TAG}",
        "${HUB}/${service}:latest"] : ["${HUB}/${service}:${TAG}"]
}

function cache-tag {
    params = [service]
    result = "${HUB}/${service}:${TAG}-cache"
}

target abs-base-image-cache {
    inherits = ["abs-base-image-cache"]
    dockerfile-inline = "FROM aerospike.jfrog.io/ecosystem-container-dev-local/abs-base-image:latest"
    platforms  = split(",", "${PLATFORMS}")
    output = ["type=cacheonly"]
}

target aerospike-backup-service {
    labels = {
        "org.opencontainers.image.title"="Aerospike Backup Service"
        "org.opencontainers.image.description"="Aerospike Backup Service provides a set of REST APIs to schedule full and incremental backups. Additionally, these APIs can be used to restore data from a backup to a cluster"
        "org.opencontainers.image.documentation"="https://github.com/aerospike/aerospike-backup-service?tab=readme-ov-file#aerospike-backup-service"
        "org.opencontainers.image.base.name"="registry.access.redhat.com/ubi9/ubi-minimal"
        "org.opencontainers.image.source"="https://github.com/aerospike/aerospike-backup-service/tree/${GIT_BRANCH}"
        "org.opencontainers.image.vendor"="Aerospike"
        "org.opencontainers.image.version"="${VERSION}"
        "org.opencontainers.image.url"="https://github.com/aerospike/aerospike-backup-service"
        "org.opencontainers.image.licenses"="Apache-2.0"
        "org.opencontainers.image.revision"="${GIT_COMMIT_SHA}"
        "org.opencontainers.image.created"="${ISO8601}"
    }
    secret = [
        "type=env,id=GITHUB_TOKEN"
    ]
    args = {
        GIT_BRANCH="${GIT_BRANCH}"
        CACHEBUST="${CACHEBUST}"
    }
    contexts = {
        base = "target:abs-base-image-cache"
    }
    dockerfile = "./Dockerfile"
    platforms  = split(",", "${PLATFORMS}")
    cache-to = ["mode=max,type=registry,ref=${cache-tag("aerospike-backup-service")}"]
    cache-from = ["type=registry,ref=${cache-tag("aerospike-backup-service")}"]

    tags = tags("aerospike-backup-service")
    output = ["type=image,push=true"]
}

secret "GITHUB_TOKEN" {
    env = "GITHUB_TOKEN"
}

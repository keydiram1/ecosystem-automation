group default {
  targets = [
    "ubuntu2204-base-image",
    "rhel9-base-image",
    "rhel8-base-image",
    "abs-base-image"
  ]
}

variable LATEST {
  default = false
}

variable TAG {
  default = ""
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

target rhel9-base-image {
  context = "./rhel9"
  dockerfile = "rhel9.Dockerfile"
  platforms  = split(",", "${PLATFORMS}")
  tags = tags("base-rhel9")
  output = ["type=image,push=true,store=true"]
}

target rhel8-base-image {
  context = "./rhel8"
  dockerfile = "rhel8.Dockerfile"
  platforms  = split(",", "${PLATFORMS}")
  tags = tags("base-rhel8")
  output = ["type=image,push=true,store=true"]
}

target ubuntu2204-base-image {
  dockerfile = "./ubuntu2204.Dockerfile"
  platforms  = split(",", "${PLATFORMS}")
  tags = tags("base-ubuntu2204")
  output = ["type=image,push=true,store=true"]
}

target abs-base-image {
  dockerfile = "./base.Dockerfile"
  platforms  = split(",", "${PLATFORMS}")
  tags = tags("abs-base-image")
  output = ["type=image,push=true,store=true"]
}

group "default" {
  targets = [
    "ecosys-jenkins-server"
  ]
}

variable PLATFORMS {
  default = "linux/amd64,linux/arm64"
}

target "ecosys-jenkins-server" {
  dockerfile = "./jenkins.Dockerfile"
  platforms = split(",", "${PLATFORMS}")

  tags = ["aerospike.jfrog.io/ecosystem-container-dev-local/ecosys-jenkins-server:latest"]

  output = ["type=image,push=true,store=true"]
}

locals {
  module_name = "asdb"
  image_families = {
    "ubuntu20.04-amd64" = "ubuntu-2004-lts"
    "ubuntu20.04-arm64" = "ubuntu-2004-lts-arm64"
    "ubuntu22.04-amd64" = "ubuntu-2204-lts"
    "ubuntu22.04-arm64" = "ubuntu-2204-lts-arm64"
    "ubuntu24.04-amd64" = "ubuntu-2404-lts-amd64"
    "ubuntu24.04-arm64" = "ubuntu-2404-lts-arm64"
    "el8-amd64"         = "rocky-linux-8"
    "el8-arm64"         = "rocky-linux-8-optimized-gcp-arm64"
    "el9-amd64"         = "rocky-linux-9"
    "el9-arm64"         = "rocky-linux-9-arm64"
    "debian11-amd64"    = "debian-11"
    "debian11-arm64"    = null
    "debian12-amd64"    = "debian-12"
    "debian12-arm64"    = "debian-12-arm64"
  }
  image_family = local.image_families["${replace(var.nodes.distro, " ", "")}-${var.nodes.arch}"]
  arch = {
    "amd64" = "x86_64"
    "arm64" = "aarch64"
  }
}

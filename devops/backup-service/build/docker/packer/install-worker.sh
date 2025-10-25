#!/bin/bash -e
cloud-init status --wait
export DEBIAN_FRONTEND=noninteractive

# Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sh get-docker.sh
usermod -aG docker "ubuntu"
rm -f get-docker.sh

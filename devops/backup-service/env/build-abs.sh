#!/bin/bash -e

REPO_URL="https://github.com/aerospike/aerospike-backup-service.git"
REPO_DIR="aerospike-backup-service"
BRANCH="${BRANCH:-v3}"

pushd .
if [ ! -d "$REPO_DIR/.git" ]; then
    echo "Cloning repository..."
    git clone --branch "$BRANCH" --single-branch "$REPO_URL"
    cd "$REPO_DIR"
else
    echo "Repository exists. Fetching and checking out branch..."
    cd "$REPO_DIR" || exit 1
    git fetch origin "$BRANCH"
    git checkout "$BRANCH"
    git reset --hard "origin/$BRANCH"
fi

make packages

cd ./build/target
if dpkg -s "$REPO_DIR" &> /dev/null; then
    echo "Removing the package"
     sudo dpkg -P "$REPO_DIR"
fi
sudo dpkg -i "$REPO_DIR"_*_amd64.deb
popd

sudo cp aerospike-backup-service.yml /etc/aerospike-backup-service/aerospike-backup-service.yml
sudo systemctl restart "$REPO_DIR"


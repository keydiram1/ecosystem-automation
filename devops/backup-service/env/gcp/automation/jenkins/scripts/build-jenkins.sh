#!/bin/bash

set -euo pipefail

readonly REGISTRY_URL="aerospike.jfrog.io"
readonly REPOSITORY="ecosystem-container-dev-local"
readonly IMAGE_NAME="ecosys-jenkins-server"
readonly IMAGE_TAG="latest"
readonly BACKUP_TAG="backup"
readonly JFROG_CONFIG_NAME="aerospike"

readonly QUERY_FILE="query.json"
readonly BASE_JENKINS_IMAGE="jenkins/jenkins:latest-jdk21"
readonly PLUGINS_FILE="plugins.txt"
readonly TEMP_FILE="tmp.txt"

readonly FULL_IMAGE_PATH="$REGISTRY_URL/$REPOSITORY/$IMAGE_NAME"
readonly LATEST_IMAGE="$FULL_IMAGE_PATH:$IMAGE_TAG"
readonly BACKUP_IMAGE="$FULL_IMAGE_PATH:$BACKUP_TAG"

if docker images --format "{{.Repository}}:{{.Tag}}" | grep -q "$BACKUP_IMAGE"; then
  docker rmi "$BACKUP_IMAGE"
fi

if docker images --format "{{.Repository}}:{{.Tag}}" | grep -q "$LATEST_IMAGE"; then
  docker rmi "$LATEST_IMAGE"
fi

if docker images --format "{{.Repository}}:{{.Tag}}" | grep -q "$BASE_JENKINS_IMAGE"; then
  docker rmi "$BASE_JENKINS_IMAGE"
fi

docker login "$REGISTRY_URL" -u "$DOCKER_USERNAME" -p "$DOCKER_PASSWORD"
docker pull "$LATEST_IMAGE"
docker tag "$LATEST_IMAGE" "$BACKUP_IMAGE"

jfrog config add "$JFROG_CONFIG_NAME" \
--user="$JFROG_USERNAME" \
--password="$JFROG_TOKEN" \
--url="https://$REGISTRY_URL" \
--artifactory-url="https://$REGISTRY_URL/artifactory" \
--distribution-url="https://$REGISTRY_URL/distribution" \
--xray-url="https://$REGISTRY_URL/xray" \
--interactive=false \
--overwrite=true

cat > "$QUERY_FILE" << EOF
{
  "files": [
    {
      "aql": {
        "items.find": {
          "repo": "$REPOSITORY",
          "path": {"\$match": "$IMAGE_NAME/*"}
        }
      }
    }
  ]
}
EOF

jfrog rt delete --spec "$QUERY_FILE" --quiet
rm "$QUERY_FILE"

docker push "$BACKUP_IMAGE"

docker run \
  --name jenkins-plugn-updater \
  --rm \
  --volume "$PWD/$PLUGINS_FILE:/usr/share/jenkins/ref/$PLUGINS_FILE:ro" \
  --interactive \
  --tty \
  "$BASE_JENKINS_IMAGE" \
  bash -c "stty -onlcr && jenkins-plugin-cli -f /usr/share/jenkins/ref/$PLUGINS_FILE --available-updates --output txt" > "$TEMP_FILE"
mv "$TEMP_FILE" "$PLUGINS_FILE"

docker build --file jenkins.Dockerfile --tag "$LATEST_IMAGE" --no-cache --progress plain .
docker push "$LATEST_IMAGE"

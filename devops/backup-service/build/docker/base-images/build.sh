#!/bin/bash -e
CHANNEL="dev"
TAG_LATEST=false
TAG=""
GROUP="image"
GIT_BRANCH="main"
PLATFORMS="linux/amd64,linux/arm64"

POSITIONAL_ARGS=()

while [[ $# -gt 0 ]]; do
  case $1 in
  --channels)
    CHANNEL="$2"
    shift
    shift
    ;;
  --tag)
    TAG="$2"
    shift
    shift
    ;;
  --tag-latest)
    TAG_LATEST="$2"
    shift
    ;;
  --platforms)
    PLATFORMS="$2"
    shift
    shift
    ;;
  --group)
    GROUP="$2"
    shift
    shift
    ;;
  --branch)
    GIT_BRANCH="$2"
    shift
    shift
    ;;
  -* | --*)
    echo "Unknown option $1"
    exit 1
    ;;
  *)
    POSITIONAL_ARGS+=("$1") # save positional arg
    shift                   # past argument
    ;;
  esac
done

set -- "${POSITIONAL_ARGS[@]}"


if [ "$CHANNEL" == "dev" ]; then
  HUB="aerospike.jfrog.io/ecosystem-container-dev-local"
elif [ "$CHANNEL" == "stage" ]; then
  HUB="aerospike.jfrog.io/ecosystem-container-stage-local"
elif [ "$CHANNEL" == "prod" ]; then
  HUB="aerospike.jfrog.io/ecosystem-container-prod-local"
else
  echo "Unknown channel"
  exit 1
fi

docker login aerospike.jfrog.io -u "$DOCKER_USERNAME" -p "$DOCKER_PASSWORD"
PLATFORMS="$PLATFORMS" TAG="$TAG" HUB="$HUB" LATEST="$TAG_LATEST" docker buildx bake "$GROUP" \
--progress plain \
--file docker-bake.hcl

#!/bin/bash
# USER="<user-name>" PASSWORD="<password>" ./promote.sh --service-name aerospike-backup-service --source-tag 0.5.0-rc18 --target-tag latest --source-channel dev --target-channel prod
set -o nounset
set -o errexit
set -o pipefail

export CI=true
WORKSPACE="$(git rev-parse --show-toplevel)"
SOURCE_CHANNEL="dev"
TARGET_CHANNEL="stage"
TARGET_TAG=""
SOURCE_TAG=""
SERVICE_NAME="aerospike-backup-service"
URL="https://aerospike.jfrog.io/artifactory"

POSITIONAL_ARGS=()

while [[ $# -gt 0 ]]; do
  case $1 in
  --service-name)
    SERVICE_NAME="$2"
    shift
    shift
    ;;
  --source-channel)
    SOURCE_CHANNEL="$2"
    shift
    shift
    ;;
  --target-channel)
    TARGET_CHANNEL="$2"
    shift
    shift
    ;;
  --target-tag)
    TARGET_TAG="$2"
    shift
    shift
    ;;
  --source-tag)
    SOURCE_TAG="$2"
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

get_channel() {
  local channel=$1
  if [ "$channel" == "dev" ]; then
    echo "ecosystem-container-dev-local"
  elif [ "$channel" == "stage" ]; then
    echo "ecosystem-container-stage-local"
  elif [ "$channel" == "prod" ]; then
    echo "ecosystem-container-prod-local"
  else
    echo "Unknown channel"
  fi
}

source_channel="$(get_channel "$SOURCE_CHANNEL")"
if [ "$source_channel" == "Unknown channel" ]; then
  exit 1
fi

target_channel="$(get_channel "$TARGET_CHANNEL")"
if [ "$target_channel" == "Unknown channel" ]; then
  exit 1
fi


jfrog rt docker-promote "$SERVICE_NAME" "$source_channel" "$target_channel" \
--source-tag "$SOURCE_TAG" \
--target-tag "$TARGET_TAG" \
--user "$USER" \
--password "$PASSWORD" \
--url "$URL" \
--copy

#!/bin/bash -e

WORKSPACE="$(git rev-parse --show-toplevel)"
PROJECT_ID="ecosystem-connectors-data"
REGION="me-west1"
ZONE="$REGION"-a
TARGET=""
MACHINE_TYPE="e2-standard-4"
LOCAL_STORAGE="false"
SOURCE_IMAGE_FAMILY="ubuntu-2404-lts-amd64"
DISK_SIZE=10
#SOURCE_IMAGE_FAMILY="ubuntu-2204-lts"

POSITIONAL_ARGS=()

while [[ $# -gt 0 ]]; do
	case $1 in
	--target)
		TARGET="$2"
		shift
		shift
		;;
	--machine-type)
		MACHINE_TYPE="$2"
		shift
		shift
		;;
	--disk-size)
		DISK_SIZE="$2"
		shift
		shift
		;;
	--local-storage)
		LOCAL_STORAGE="$2"
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

if [ -z "$TARGET" ]; then
	echo "--target parameter is empty"
	exit 1
fi

IMAGE_NAME="$(echo "$TARGET" | awk -F: '{print $NF}')"

echo "$IMAGE_NAME"

if [ -n "$(gcloud compute images list \
	--project="$PROJECT_ID" \
	--format="value(NAME)" \
	--filter="name=('$IMAGE_NAME')" \
	--quiet)" ]; then

	gcloud compute images delete "$IMAGE_NAME" --project="$PROJECT_ID" --quiet
fi

packer build \
	-var "project_id=$PROJECT_ID" \
	-var "zone=$ZONE" \
	-var "region=$REGION" \
	-var "local_storage=$LOCAL_STORAGE" \
	-var "image_name=$IMAGE_NAME" \
	-var "machine_type=$MACHINE_TYPE" \
	-var "github_token=$GITHUB_TOKEN" \
	-var "source_image_family=$SOURCE_IMAGE_FAMILY" \
	-var "disk_size=$DISK_SIZE" \
	"$WORKSPACE"/devops/backup-service/env/gcp/automation/jenkins/packer/image.pkr.hcl

#-var "startup_script_file=$WORKSPACE/devops/backup-service/env/gcp/automation/jenkins/packer/scripts/startup.sh" \

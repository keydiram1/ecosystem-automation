#!/bin/bash

# Configuration variables
PROJECT="ecosystem-connectors-data"
ZONE="me-west1-a"
CLUSTER_PREFIX="ws-1-asdb"

# Get list of matching disks
DISK_LIST=$(gcloud compute disks list \
	--project="$PROJECT" \
	--filter="name~^${CLUSTER_PREFIX}-node-[0-9]+-shadow-[0-9]+ AND zone:($ZONE)" \
	--format="value(name)")

if [[ -z "$DISK_LIST" ]]; then
	echo "No matching disks found for prefix '$CLUSTER_PREFIX' in zone '$ZONE'."
	exit 0
fi

# Loop over disks and delete
for DISK_NAME in $DISK_LIST; do
	echo "Deleting disk: $DISK_NAME"
	gcloud compute disks delete "$DISK_NAME" \
		--project="$PROJECT" \
		--zone="$ZONE" \
		--quiet
done

echo "All matching disks deleted."

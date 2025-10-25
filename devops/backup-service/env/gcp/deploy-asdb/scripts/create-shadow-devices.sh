#!/bin/bash

# Configuration variables
PROJECT="ecosystem-connectors-data"
DISK_TYPE="pd-balanced"
DISK_SIZE="375GB"
ZONE="me-west1-a"
CLUSTER_PREFIX="ws-1-asdb"
NODES=5
DISKS_PER_NODE=2

# Loop over nodes and disks
for NODE in $(seq 0 $((NODES - 1))); do
	for DISK in $(seq 0 $((DISKS_PER_NODE - 1))); do
		DISK_NAME="${CLUSTER_PREFIX}-node-${NODE}-shadow-${DISK}"
		echo "Creating disk: $DISK_NAME"
		gcloud compute disks create "$DISK_NAME" \
			--project="$PROJECT" \
			--type="$DISK_TYPE" \
			--size="$DISK_SIZE" \
			--zone="$ZONE"
	done
done

#!/bin/bash -e

AMI_NAME="arm-based-builder"

IMAGE_ID="$(aws ec2 describe-images \
--filters "Name=name,Values=$AMI_NAME" --query 'Images[0].ImageId' --output text)"
echo "$IMAGE_ID"

SNAPSHOT_IDS="$(aws ec2 describe-images \
--image-ids "$IMAGE_ID" \
--query 'Images[0].BlockDeviceMappings[*].Ebs.SnapshotId' --output json | jq -r '.[]')"

aws ec2 deregister-image --image-id "$IMAGE_ID"

for SNAPSHOT_ID in "${SNAPSHOT_IDS[@]}"; do
    echo "$SNAPSHOT_ID"
    aws ec2 delete-snapshot --snapshot-id "$SNAPSHOT_ID"
done

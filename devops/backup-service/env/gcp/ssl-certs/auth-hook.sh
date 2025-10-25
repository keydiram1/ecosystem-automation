#!/bin/bash

set -e

PROJECT_ID="ecosystem-connectors-data"
REGION="me-west1"
MANAGED_ZONE="ecoeng-dev"

DOMAIN="${CERTBOT_DOMAIN}"
VALIDATION="${CERTBOT_VALIDATION}"

RECORD_NAME="_acme-challenge.${DOMAIN}."
RECORD_TYPE="TXT"
TTL="300"

echo "Adding TXT record: ${RECORD_NAME} with value ${VALIDATION}"

gcloud dns record-sets transaction start --project="$PROJECT_ID" --zone="$MANAGED_ZONE"

EXISTING=$(gcloud dns record-sets list --project="$PROJECT_ID" --zone="$MANAGED_ZONE" --name="$RECORD_NAME" --type="$RECORD_TYPE" --format="value(rrdatas[0])")
if [ -n "$EXISTING" ]; then
	echo "Removing old value: $EXISTING"
	gcloud dns record-sets transaction remove --project="$PROJECT_ID" --zone="$MANAGED_ZONE" \
		--name="$RECORD_NAME" --type="$RECORD_TYPE" --ttl="$TTL" "$EXISTING"
fi

gcloud dns record-sets transaction add --project="$PROJECT_ID" --zone="$MANAGED_ZONE" \
	--name="$RECORD_NAME" --type="$RECORD_TYPE" --ttl="$TTL" "$VALIDATION"

gcloud dns record-sets transaction execute --project="$PROJECT_ID" --zone="$MANAGED_ZONE"

echo "Waiting 30 seconds for DNS to propagate..."
sleep 30

echo "Done adding DNS record."

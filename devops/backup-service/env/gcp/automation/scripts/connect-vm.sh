#!/bin/bash

PROJECT_ID="ecosystem-connectors-data"
ZONE="me-west1-a"
USER="ubuntu"

INSTANCE_ID="$(gcloud compute instances list \
	--filter="name~'^ecosys|^jenkins-worker'" \
	--project="$PROJECT_ID" | awk 'NR > 1' | fzf | awk '{print $1}')"

gcloud compute ssh --project "$PROJECT_ID" --zone "$ZONE" "$USER@$INSTANCE_ID" --tunnel-through-iap

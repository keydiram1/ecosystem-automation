#!/bin/bash

INSTANCE_ID="$(gcloud compute instances list \
--filter="name~'^ecoeng'" \
--project=ecosystem-connectors-data | awk 'NR > 1' | fzf | awk '{print $1}')"

gcloud compute ssh --project "ecosystem-connectors-data" --zone "me-west1-a" "ubuntu@$INSTANCE_ID" --tunnel-through-iap

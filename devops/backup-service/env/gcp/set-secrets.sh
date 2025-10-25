#!/bin/bash
PROJECT_ID="ecosystem-connectors-data"
API="secretmanager.googleapis.com"

if ! gcloud services list --enabled --filter="$API" --project="$PROJECT_ID" | grep -q "$API"; then
	gcloud services enable "$API" --project="$PROJECT_ID"
fi

if gcloud secrets describe "docker_username" --quiet; then
	gcloud secrets delete "docker_username" --quiet
fi

if gcloud secrets describe "docker_password" --quiet; then
	gcloud secrets delete "docker_password" --quiet
fi

gcloud secrets create docker_username --project="$PROJECT_ID" --replication-policy="automatic"
gcloud secrets create docker_password --project="$PROJECT_ID" --replication-policy="automatic"
echo -n "$DOCKER_USERNAME" | gcloud secrets versions add "docker_username" --project="$PROJECT_ID" --data-file=-
echo -n "$DOCKER_PASSWORD" | gcloud secrets versions add "docker_password" --project="$PROJECT_ID" --data-file=-

docker login aerospike.jfrog.io \
	-u "$(gcloud secrets versions access latest --project="ecosystem-connectors-data" --secret="docker_username")" \
	-p "$(gcloud secrets versions access latest --project="ecosystem-connectors-data" --secret="docker_password")"

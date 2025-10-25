#!/bin/bash

PROJECT_ID="ecosystem-connectors-data"
REGION="me-west1"
ZONE="$REGION"-a
USER="ubuntu"
GITHUB_TOKEN="$(gcloud secrets versions access latest \
	--project="$PROJECT_ID" \
	--secret="github-token")"
GITHUB_USERNAME="user"

gcloud compute ssh \
	--project="$PROJECT_ID" \
	--zone="$ZONE" \
	--tunnel-through-iap "$USER@$(gcloud compute instances list \
		--project="$PROJECT_ID" \
		--filter="name~'^ecosys'" \
		--format="get(name)" \
		--limit=1)" \
	--command="bash -s" <<-EOF
		  GITHUB_TOKEN="${GITHUB_TOKEN}" \
		  GITHUB_USERNAME="${GITHUB_USERNAME}" \
		  REGION="${REGION}" \
		  ZONE="${ZONE}" \
		  PROJECT_ID="${PROJECT_ID}" \
		  CASC_JENKINS_CONFIG="/usr/share/jenkins" \
		  DOCKER="\$(which docker)" \
		  SA_KEY="\$(cat key.json | base64 | tr '\n' ' ')" \
		  docker compose -f docker-compose-jenkins.yml restart
	EOF

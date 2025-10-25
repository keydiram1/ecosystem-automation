#!/bin/bash
PROJECT_ID="ecosystem-connectors-data"

kubectl create secret docker-registry regcred \
--docker-server=aerospike.jfrog.io \
--docker-username="$(gcloud secrets versions access latest \
--secret="docker_username" \
--project="$PROJECT_ID")" \
--docker-password="$(gcloud secrets versions access latest \
--secret="docker_password" \
--project="$PROJECT_ID")" \
--namespace aerospike || true

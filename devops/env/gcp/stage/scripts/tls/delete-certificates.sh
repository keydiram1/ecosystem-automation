#!/bin/bash -e

secrets=(
  "password.txt"
  "encryption.key.pem"
  "ca.aerospike.com.key"
  "ca.aerospike.com.pem"
  "asd.aerospike.com.key"
  "asd.aerospike.com.pem"
  "abs.aerospike.com.key"
  "abs.aerospike.com.pem"
  "client.aerospike.com.key"
  "client.aerospike.com.pem"
)

PROJECT_ID="ecosystem-connectors-data"
REGION="me-west1"

for secret in "${secrets[@]}"; do
  secret_id="$(echo "$secret" | tr '.' '-')"

  if gcloud secrets describe "$secret_id" --project="$PROJECT_ID" --quiet; then
      echo "Deleting existing secret: $secret_id"
      gcloud secrets delete "$secret_id" --project="$PROJECT_ID" --quiet
  fi
done

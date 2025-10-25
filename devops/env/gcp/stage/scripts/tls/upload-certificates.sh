#!/bin/bash -e

secrets=(
  "password.txt"
  "encryption.key.pem"
  "ca.aerospike.com.key"
  "ca.aerospike.com.pem"
  "ca.aerospike.com.pem.jks"
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
  file="$(pwd)/output/$secret"
  secret_id="$(basename "$file" | tr '.' '-')"

  if [[ -f "$file" ]]; then
      if gcloud secrets describe "$secret_id" --project="$PROJECT_ID" --quiet; then
        echo "Deleting existing secret: $secret_id"
        gcloud secrets delete "$secret_id" --project="$PROJECT_ID" --quiet
      fi

      echo "Creating secret: $secret_id in region: $REGION"

      if [ "$secret" == "ca.aerospike.com.pem" ]; then
        gcloud secrets create "$secret_id" \
        --project="$PROJECT_ID" \
        --replication-policy="user-managed" \
        --locations="$REGION" \
        --labels=jenkins-credentials-type=file,jenkins-credentials-filename=ca-aerospike-com,jenkins-credentials-file-extension=pem,environment=jenkins,resource=asdb
      elif [ "$secret" == "ca.aerospike.com.pem.jks" ]; then
        gcloud secrets create "$secret_id" \
        --project="$PROJECT_ID" \
        --replication-policy="user-managed" \
        --locations="$REGION" \
        --labels=jenkins-credentials-type=file,jenkins-credentials-filename=ca-aerospike-com-jks,jenkins-credentials-file-extension=jks,environment=jenkins,resource=asdb
      else
        gcloud secrets create "$secret_id" \
        --project="$PROJECT_ID" \
        --replication-policy="user-managed" \
        --locations="$REGION" \
        --labels=resource=asdb
      fi

      echo "Adding secret version for: $secret_id"
      gcloud secrets versions add "$secret_id" --project="$PROJECT_ID" --data-file="$file"
  else
      echo "File not found: $file, skipping secret creation for $secret"
  fi
done

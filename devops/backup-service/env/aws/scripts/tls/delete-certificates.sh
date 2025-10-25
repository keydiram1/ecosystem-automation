#!/bin/bash -e

for secret in $(aws secretsmanager list-secrets --output json | \
jq -r '.SecretList[] | select(.Name | startswith("testenv")) | .Name'); do
  aws secretsmanager delete-secret --secret-id "$secret" --force-delete-without-recovery
done

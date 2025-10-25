#!/bin/bash

docker compose -v -f docker-compose.yaml down
terraform -chdir="./k8s" destroy -auto-approve -no-color
terraform -chdir="./kind" destroy -auto-approve -no-color
rm -v -r ./aerospike
rm -v ./kind/test-cluster-config

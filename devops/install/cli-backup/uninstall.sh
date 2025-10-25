#!/bin/bash
source .env

docker rm -f aerospike-source
docker compose down
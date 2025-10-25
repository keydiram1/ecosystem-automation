#!/bin/bash
source .env

docker rm -f aerospike-source
docker rm -f minio
docker compose down
docker rm -f backup-service
docker rmi backup-service
docker rm -f aerospike-source2
rm "$(pwd)/aerospike-backup-service.log"
docker volume rm conf_directory
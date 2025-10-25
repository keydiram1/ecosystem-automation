#!/bin/bash
source .env

docker rm -f aerospike-source
docker rm -f aerospike-source2
docker rm -f aerospike-source3
docker rm -f aerospike-backup
docker rm -f minio
docker compose down
docker compose -f docker-compose-backup.yml down

if [ $PULL_FROM_DOCKER_REPOSITORY = false ]
then
	docker compose -f $PATH_TO_BACKUP_PROJECT/docker-compose.yml down
fi

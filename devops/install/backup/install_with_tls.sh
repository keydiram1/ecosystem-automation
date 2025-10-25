#!/bin/bash
source .env

echo "Install minio"
docker compose -p minio up -d

if [ $STATIC_CONFIGURATION = true ]; then
  confFile=aerospike.conf
  else
    confFile=dynamic/aerospike.conf
    if [ $(uname) = Darwin ]; then
       sed -i "" "s/AEROSPIKE_REST_BACKEND_DYNAMICXDRMODE.*/AEROSPIKE_REST_BACKEND_DYNAMICXDRMODE: 'true'/g" docker-compose.local.yml
       else
          sed -i "s/AEROSPIKE_REST_BACKEND_DYNAMICXDRMODE.*/AEROSPIKE_REST_BACKEND_DYNAMICXDRMODE: 'true'/g" docker-compose.local.yml
          sed -i "s/AEROSPIKE_REST_BACKEND_DYNAMICXDRMODE.*/AEROSPIKE_REST_BACKEND_DYNAMICXDRMODE: 'true'/g" /opt/automation/localInstallation/enterprise-backup/docker-compose.local.yml
    fi
fi

if [ $PULL_FROM_DOCKER_REPOSITORY = false ]; then
  cp -Rf docker/* $PATH_TO_BACKUP_PROJECT/docker
  mkdir -p $PATH_TO_BACKUP_PROJECT/docker/conf/tls
  cp -Rf conf/tls/* $PATH_TO_BACKUP_PROJECT/docker/conf/tls
  rm $PATH_TO_BACKUP_PROJECT/docker/xdr-proxy*
  cp -f .env $PATH_TO_BACKUP_PROJECT/docker
  cp -f docker-compose.local.backup.enterprise.tls.yml $PATH_TO_BACKUP_PROJECT
  cp -f conf/slave/settings.xml $PATH_TO_BACKUP_PROJECT
  cp -f docker/xdr-proxy-host.docker.internal.yml $PATH_TO_BACKUP_PROJECT/docker/aerospike-xdr-proxy.yml

  cd $PATH_TO_BACKUP_PROJECT/docker
fi

echo "Generate aerospike source certificates"
./conf/tls/generate-certs-source.sh

echo "Generate aerospike backup certificates"
./conf/tls/generate-certs-backup.sh

echo "Generate aerospike xdr proxy certificates"
./conf/tls/generate-certs-xdr-proxy.sh

echo "Install aerospike source with TLS"
docker run -d --name aerospike-source -p 3000-3002:3000-3002 \
-v $PWD/conf/tls/:/etc/aerospike/ \
-v $PWD/conf/tls/source:/opt/aerospike/etc \
-v $PWD/conf/tls/xdrproxy:/opt/aerospike/etc/xdrproxy \
--add-host=host.docker.internal:host-gateway \
$AEROSPIKE_IMAGE_NAME:$ASDB_VERSION --config-file /etc/aerospike/source/aerospike.conf

echo "Install aerospike backup with TLS"
docker run -d --name aerospike-backup -p 3004-3007:3004-3007 \
-v $PWD/conf/tls/:/etc/aerospike/ \
-v $PWD/conf/tls/backup:/opt/aerospike/etc \
--add-host=host.docker.internal:host-gateway \
$AEROSPIKE_IMAGE_NAME:$AEROSPIKE_BACKUP_IMAGE_VERSION --config-file /etc/aerospike/backup/aerospike.conf

echo "Install aerospike source certificates"
./conf/tls/install-certs-source.sh

echo "Install aerospike backup certificates"
./conf/tls/install-certs-backup.sh

echo "Install aerospike xdr proxy certificates"
./conf/tls/install-certs-xdr-proxy.sh



echo "Add user to source"
echo "At the first run you should install https://download.aerospike.com/artifacts/aerospike-tools/7.3.1/aerospike-tools-7.3.1-macOS.pkg"
# Use docker exec because TLS doesn't work well with ASADM locally (potential bug of ASADM)
docker exec aerospike-source asadm -U admin -P admin -h 127.0.0.1:source.server:3000 --tls-enable --tls-cafile=/opt/aerospike/etc/certs/source.ca.crt -e 'enable; manage acl create user tester password psw roles truncate sindex-admin user-admin \
data-admin read-write read write read-write-udf sys-admin udf-admin'

echo "Add user to backup"
# Use docker exec because TLS doesn't work well with ASADM locally (potential bug of ASADM)
docker exec aerospike-backup asadm -U admin -P admin -h 127.0.0.1:backup.server:3004 --tls-enable --tls-cafile=/opt/aerospike/etc/certs/backup.ca.crt -e 'enable; manage acl create user tester password psw roles truncate sindex-admin user-admin \
data-admin read-write read write read-write-udf sys-admin udf-admin'

if [ $PULL_FROM_DOCKER_REPOSITORY = false ]
then
    cd $PATH_TO_BACKUP_PROJECT
    docker compose -f docker-compose.yml -f docker-compose.local.yml -f docker-compose.local.backup.enterprise.tls.yml up -d
fi
if [ $PULL_FROM_DOCKER_REPOSITORY = true ]
then
  DOCKER_USERNAME=$(aws secretsmanager get-secret-value --secret-id adr/jfrog/testenv/creds --output json | jq -r '.SecretString' | jq '.username')
  DOCKER_PASSWORD=$(aws secretsmanager get-secret-value --secret-id adr/jfrog/testenv/creds --output json | jq -r '.SecretString' | jq '.password')
  docker login aerospike.jfrog.io -u ${DOCKER_USERNAME//\"/} -p ${DOCKER_PASSWORD//\"/}
	if [ $PULL_BACKUP_IMAGES = true ]
	then
	   echo "Pulling backup images"
	   docker compose -f docker-compose-backup.yml pull
	fi
	docker compose -f docker-compose-backup.yml -f docker-compose.local.yml -f docker-compose.local.tls.yml up -d
fi
counter=0
until $(curl --output /dev/null --silent --head --fail http://localhost:8080); do
	if [ $counter -gt 30 ]
	then
		echo 'adr-rest-backend failed to start within 150 seconds'
		break
	fi
    echo 'Waiting for rest-backend to be available in http://localhost:8080'
    ((counter++))
    sleep 5
done

docker rm -f createbucket


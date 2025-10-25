#!/bin/bash
source .env

if [ $TLS_ENABLED = true ]; then
  echo "TLS enabled. Running the install_with_tls.sh script"
  source ./install_with_tls.sh
  exit 0
fi

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

echo "Install aerospike source"
docker run -d --name aerospike-source -p 3000-3002:3000-3002 \
  -v $PWD/conf/:/etc/aerospike/ --add-host=host.docker.internal:host-gateway \
  $AEROSPIKE_IMAGE_NAME:$ASDB_VERSION --config-file /etc/aerospike/source/$confFile

echo "Install aerospike backup"
docker run -d --name aerospike-backup -p 3004-3007:3004-3007 \
  -v $PWD/conf/:/etc/aerospike/ --add-host=host.docker.internal:host-gateway \
  $AEROSPIKE_IMAGE_NAME:$AEROSPIKE_BACKUP_IMAGE_VERSION --config-file /etc/aerospike/backup/aerospike.conf

echo "Install minio"
docker compose -p minio up -d

echo "Add user to source"
echo "At the first run you should install https://download.aerospike.com/artifacts/aerospike-tools/7.3.1/aerospike-tools-7.3.1-macOS.pkg"
asadm -U admin -P admin -p 3000 -e 'enable; manage acl create user tester password psw roles truncate sindex-admin user-admin \
data-admin read-write read write read-write-udf sys-admin udf-admin'

echo "Add user to backup"
asadm -U admin -P admin -p 3004 -e 'enable; manage acl create user tester password psw roles truncate sindex-admin user-admin \
data-admin read-write read write read-write-udf sys-admin udf-admin'

if [ $INSTALL_TWO_AS_SOURCE_CLUSTERS = true ]; then
  source ./install_second_as_source.sh
fi

if [ $INSTALL_3_NODES_SOURCE_CLUSTER = true ]; then
  source ./install_3_nodes_source_cluster.sh
fi

if [ $PULL_FROM_DOCKER_REPOSITORY = false ]; then
  cp -Rf docker/* $PATH_TO_BACKUP_PROJECT/docker
  rm $PATH_TO_BACKUP_PROJECT/docker/xdr-proxy*
  rm -rf $PATH_TO_BACKUP_PROJECT/docker/tls
  cp -f .env $PATH_TO_BACKUP_PROJECT/docker
  cp -f conf/slave/settings.xml $PATH_TO_BACKUP_PROJECT
  cp -f docker/xdr-proxy-host.docker.internal.yml $PATH_TO_BACKUP_PROJECT/docker/aerospike-xdr-proxy.yml

  cd $PATH_TO_BACKUP_PROJECT
  docker compose -f docker-compose.yml -f docker-compose.local.yml up -d
fi

if [ $PULL_FROM_DOCKER_REPOSITORY = true ]; then
  DOCKER_USERNAME=$(aws secretsmanager get-secret-value --secret-id adr/jfrog/testenv/creds --output json | jq -r '.SecretString' | jq '.username')
  DOCKER_PASSWORD=$(aws secretsmanager get-secret-value --secret-id adr/jfrog/testenv/creds --output json | jq -r '.SecretString' | jq '.password')
  docker login aerospike.jfrog.io -u ${DOCKER_USERNAME//\"/} -p ${DOCKER_PASSWORD//\"/}
  if [ $PULL_BACKUP_IMAGES = true ]; then
    echo "Pulling backup images"
    docker compose -f docker-compose-backup.yml pull
  fi
  docker compose -f docker-compose-backup.yml -f docker-compose.local.yml up -d
fi

for port in 8080 8081 8083 8084 8085 8086 8087; do
  counter=0
  while [[ "$(curl -s -o /dev/null -w ''%{http_code}'' localhost:$port)" != "200" ]]
  do
    if [ $counter -gt 30 ]; then
      echo "Service on port $port failed to start within 150 seconds"
      break
    fi
    echo "Waiting for service to be available on port $port"
    ((counter++))
    sleep 5
  done
  echo "Service on port $port is available"
done

docker rm -f createbucket

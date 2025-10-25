#!/bin/bash
source .env
set -e

echo "Install aerospike source"

versionPrefix="${ASDB_VERSION%%.*}"

docker run -d --rm --name aerospike-source -p 3000-3002:3000-3002 \
  -v $PWD/conf/:/etc/aerospike/ --add-host=host.docker.internal:host-gateway \
  $AEROSPIKE_IMAGE_NAME:$ASDB_VERSION --config-file /etc/aerospike/source/V$versionPrefix/aerospike.conf

if [[ "$SERVICE_CONF_FILE" == *"Remote"* ]]; then
  remoteConfig="-r"
else
  remoteConfig=""
fi

if [ "$CONFIGURATION_FILE" = "local" ]; then
  CONFIGURATION_FILE="/etc/aerospike-backup-service/aerospike-backup-service.yml"
elif [ "$CONFIGURATION_FILE" = "http" ] && [ -z "$remoteConfig" ]; then
  CONFIGURATION_FILE="http://host.docker.internal:9000/as-backup-bucket/config.yml"
elif [ "$CONFIGURATION_FILE" = "http" ] && [ "$remoteConfig" = "-r" ]; then
  CONFIGURATION_FILE="/etc/aerospike-backup-service/aerospike-backup-service.yml"
elif [ "$CONFIGURATION_FILE" = "s3" ]; then
  CONFIGURATION_FILE="/etc/aerospike-backup-service/aerospike-backup-service.yml"
fi

echo "Install minio"
docker compose -p minio up -d

sleep 2
echo "Add user to source"
echo "At the first run, you should install Aerospike tools:"
echo "https://download.aerospike.com/artifacts/aerospike-tools/7.3.1/aerospike-tools-7.3.1-macOS.pkg"
asadm -U admin -P admin -p 3000 -e 'enable; manage acl grant user admin roles service-ctrl truncate sindex-admin data-admin sys-admin udf-admin'
asadm -U admin -P admin -p 3000 -e 'enable; manage acl create user tester password psw roles truncate sindex-admin user-admin \
data-admin read-write read write read-write-udf sys-admin udf-admin'


# Roster Configuration Steps
if [ "$versionPrefix" -eq 8 ]; then
  echo "Configuring Roster for versionPrefix=8"
  for i in {1..23}; do
    NAMESPACE="source-ns$i"

    # Step 1: Check the Roster
    ROSTER=$(asinfo -U admin -P admin -p 3000 -v "roster:namespace=$NAMESPACE")
    echo "Current Roster for $NAMESPACE: $ROSTER"

    # Step 2: Extract NODE_ID from observed_nodes using portable sed
    NODE_ID=$(echo "$ROSTER" | sed -n 's/.*observed_nodes=\([0-9A-F][0-9A-F]*\).*/\1/p')
    echo "Extracted NODE_ID for $NAMESPACE: $NODE_ID"

    if [ -z "$NODE_ID" ]; then
      echo "ERROR: Could not extract NODE_ID for $NAMESPACE."
      exit 1
    fi

    # Step 3: Set the Node in the Roster
    echo "Setting roster for $NAMESPACE..."
    asinfo -U admin -P admin -p 3000 -v "roster-set:namespace=$NAMESPACE;nodes=$NODE_ID"
    if [ $? -ne 0 ]; then
      echo "ERROR: roster-set failed for $NAMESPACE."
      exit 1
    fi

    # Step 4: Recluster the Namespace
    echo "Reclustering $NAMESPACE..."
    asinfo -U admin -P admin -p 3000 -v "recluster:namespace=$NAMESPACE"
    if [ $? -ne 0 ]; then
      echo "ERROR: recluster failed for $NAMESPACE."
      exit 1
    fi

    echo "Roster configured for $NAMESPACE"
  done

  echo "Roster configuration complete for versionPrefix=8"
else
  echo "Skipping Roster Configuration: versionPrefix is not 8"
fi

sleep 5

if [ "$INSTALL_TWO_SOURCE_CLUSTERS" = true ]; then
  ./installSecondCluster.sh
fi

if [ "$(uname -s)" = "Linux" ]; then
   IMAGE_NAME="aerospike/$SERVICE_NAME:$ABS_VERSION"
else
   if [ "$PULL_FROM_JFROG" = true ]; then
       IMAGE_NAME="aerospike.jfrog.io/ecosystem-container-$ABS_JFROG_REPOSITORY_STAGE-local/aerospike-backup-service:$ABS_VERSION"
   else
       IMAGE_NAME="aerospike/aerospike-backup-service"
       if [ "$RECREATE_LOCAL_ABS_IMAGE" = true ]; then
           echo "Create backup-service image"
           current_directory=$(pwd)
           cd /tmp
           rm -rf aerospike-backup-service
           git clone https://github.com/aerospike/aerospike-backup-service.git
           cd aerospike-backup-service
           git checkout $ABS_BRANCH
           IMAGE_TAG=latest make docker-build
           cd "$current_directory"
       fi
   fi
fi

networkHost=""
if [[ "$(uname)" == "Linux" ]]; then
  networkHost="--network=host"
fi

MNT_DATA_DIR="$PWD/mnt"
mkdir -p "$MNT_DATA_DIR"
TMP_CONF_DIR="$(mktemp -d)"

chmod -R a+rwX "$MNT_DATA_DIR"
chmod -R a+rwX "$TMP_CONF_DIR"
chmod -R a+rwX "$(pwd)/conf/service"
chmod -R a+rwX "$(pwd)/encryptionKey"
touch "$(pwd)/aerospike-backup-service.log"
chmod a+rw "$(pwd)/aerospike-backup-service.log"

echo docker run --detach \
        --rm \
       --user "$(id -u):$(id -g)" \
       --env AWS_SHARED_CREDENTIALS_FILE="/etc/aws/credentials" \
       --env BACKUP_ENCRYPTION_KEY="$(cat $PWD/encryptionKey)" \
       --publish 8080-8085:8080-8085 \
       $networkHost \
       --volume "$(pwd)/conf/service/$SERVICE_CONF_FILE:/etc/aerospike-backup-service/aerospike-backup-service.yml" \
       --volume "$(pwd)/conf/service:/opt" \
       --volume "$(pwd)/conf/service/credentials:/etc/aws/credentials" \
       --volume "$(pwd)/encryptionKey:/encryptionKey" \
       --volume "$MNT_DATA_DIR:/data" \
       --volume "$TMP_CONF_DIR:/etc/aerospike-backup-service/conf.d" \
       --volume "$(pwd)/aerospike-backup-service.log:/var/log/aerospike-backup-service/aerospike-backup-service.log" \
       --add-host host.docker.internal:host-gateway \
       --name backup-service \
       "$IMAGE_NAME" \
       --config "$CONFIGURATION_FILE" \
       "$remoteConfig"

docker run --detach \
  --user "$(id -u):$(id -g)" \
  --env AWS_SHARED_CREDENTIALS_FILE="/etc/aws/credentials" \
  --env BACKUP_ENCRYPTION_KEY="$(cat $PWD/encryptionKey)" \
  --publish 8080-8085:8080-8085 \
  $networkHost \
  --volume "$(pwd)/conf/service/$SERVICE_CONF_FILE:/etc/aerospike-backup-service/aerospike-backup-service.yml" \
  --volume "$(pwd)/conf/service:/opt" \
  --volume "$(pwd)/conf/service/credentials:/etc/aws/credentials" \
  --volume "$(pwd)/encryptionKey:/encryptionKey" \
  --volume "$MNT_DATA_DIR:/data" \
  --volume "$TMP_CONF_DIR:/etc/aerospike-backup-service/conf.d" \
  --volume "$(pwd)/aerospike-backup-service.log:/var/log/aerospike-backup-service/aerospike-backup-service.log" \
  --add-host host.docker.internal:host-gateway \
  --name backup-service \
  "$IMAGE_NAME" \
  --config "$CONFIGURATION_FILE" \
  "$remoteConfig"

docker rm -f createbucket

function check_server_health {
  WAIT_INTERVAL=1
  MAX_RETRIES=20
  local retry_count=0

  while [ $retry_count -lt $MAX_RETRIES ]; do
    response_code=$(curl -s -o /dev/null -w '%{http_code}' "http://localhost:8080/health")
    echo "Health response code $response_code"

    if [ "$response_code" -eq 200 ]; then
      echo "Backup service is up and running."
      return 0
    else
      echo "Backup service not ready yet. Retrying in $WAIT_INTERVAL seconds..."
      sleep $WAIT_INTERVAL
      retry_count=$((retry_count + 1))
    fi
  done

  echo "Backup service did not start within the specified time."
  exit 1
}

set +e
check_server_health

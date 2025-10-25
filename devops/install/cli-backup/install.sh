#!/bin/bash
source .env
set -e

if [ "$LOCAL_TLS_ENABLED" != "true" ]; then
# AWS Credentials Setup
mkdir -p "$HOME/.aws"
cat <<-EOF >"$HOME/.aws/credentials"
[minio]
aws_access_key_id=minioadmin
aws_secret_access_key=minioadmin
EOF

if [ "$IS_RUNNING_ON_LOCAL_3_NODES_ENV" != "true" ]; then
    # Install MinIO
    echo "Install MinIO"
    docker compose -p minio up -d

    # Install Aerospike Source
    echo "Install Aerospike Source"
    versionPrefix="${ASDB_VERSION%%.*}"


    docker run -d --name aerospike-source -p 3000-3002:3000-3002 \
      -v $PWD/conf/:/etc/aerospike/ --add-host=host.docker.internal:host-gateway \
      --add-host=host.docker.internal:host-gateway \
      $AEROSPIKE_IMAGE_NAME:$ASDB_VERSION --config-file /etc/aerospike/source/V$versionPrefix/aerospike.conf

    sleep 5

    # Add User to Source
    echo "Add user to source"
    echo "At the first run, you should install Aerospike tools:"
    echo "https://download.aerospike.com/artifacts/aerospike-tools/7.3.1/aerospike-tools-7.3.1-macOS.pkg"
    asadm -U admin -P admin -p 3000 -e 'enable; manage acl grant user admin roles service-ctrl truncate sindex-admin data-admin sys-admin udf-admin'
    asadm -U admin -P admin -p 3000 -e 'enable; manage acl create user tester password psw roles truncate sindex-admin user-admin \
    data-admin read-write read write read-write-udf sys-admin udf-admin'
  fi
fi

# Clone aerospike-backup-cli Repository
echo "Clone and build aerospike-backup-cli"
rm -rf aerospike-backup-cli
git clone https://github.com/aerospike/aerospike-backup-cli.git

cd aerospike-backup-cli
git checkout $BACKUP_BRANCH

# Build Backup and Restore Tools
cd cmd/asbackup
make build

cd ../asrestore
make build

cd ../../..

# Copy Encryption Keys
cp encryptionKey ./aerospike-backup-cli/cmd/asbackup
cp encryptionKey ./aerospike-backup-cli/cmd/asrestore

if [ "$IS_RUNNING_ON_LOCAL_3_NODES_ENV" != "true" ]; then
  # Roster Configuration Steps
  if [ "$versionPrefix" -eq 8 ]; then
    echo "Configuring Roster for versionPrefix=8"
    for i in {1..20}; do
      NAMESPACE="source-ns$i" # Namespace will iterate from source-ns1 to source-ns20

      # Step 1: Check the Roster
      ROSTER=$(asinfo -U admin -P admin -p 3000 -v "roster:namespace=$NAMESPACE")
      echo "Current Roster for $NAMESPACE: $ROSTER"

      # Step 2: Set the Node in the Roster
      NODE_ID=$(echo "$ROSTER" | sed -n 's/.*observed_nodes=\([0-9A-F][0-9A-F]*\).*/\1/p')
      echo "Extracted NODE_ID for $NAMESPACE: $NODE_ID"

      asinfo -U admin -P admin -p 3000 -v "roster-set:namespace=$NAMESPACE;nodes=$NODE_ID"
      echo "Node $NODE_ID set in roster for namespace $NAMESPACE"

      # Step 3: Recluster the Namespace
      asinfo -U admin -P admin -p 3000 -v "recluster:namespace=$NAMESPACE"
      echo "Reclustered namespace $NAMESPACE"
    done

    echo "Roster configuration complete for versionPrefix=8"
  else
    echo "Skipping Roster Configuration: versionPrefix is not 8"
  fi
fi
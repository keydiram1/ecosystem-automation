#!/bin/bash
source .env

echo "Install second aerospike source"
docker run -d --name aerospike-source2 -p 3011-3013:3011-3013 \
  -v $PWD/conf/:/etc/aerospike/ --add-host=host.docker.internal:host-gateway \
  $AEROSPIKE_IMAGE_NAME:$ASDB_VERSION --config-file /etc/aerospike/cluster/source2/aerospike.conf

echo "Install third aerospike source"
docker run -d --name aerospike-source3 -p 3014-3016:3014-3016 \
  -v $PWD/conf/:/etc/aerospike/ --add-host=host.docker.internal:host-gateway \
  $AEROSPIKE_IMAGE_NAME:$ASDB_VERSION --config-file /etc/aerospike/cluster/source3/aerospike.conf

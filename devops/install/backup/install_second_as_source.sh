#!/bin/bash
source .env

echo "Install second aerospike source"
docker run -d --name aerospike-source2 -p 3008-3010:3008-3010 \
  -v $PWD/conf/:/etc/aerospike/ --add-host=host.docker.internal:host-gateway \
  $AEROSPIKE_IMAGE_NAME:$ASDB_VERSION --config-file /etc/aerospike/source2/aerospike.conf

sleep 3

echo "Add user to second aerospike source"
asadm -U admin -P admin -p 3008 -e 'enable; manage acl create user tester password psw roles truncate sindex-admin user-admin \
data-admin read-write read write read-write-udf sys-admin udf-admin'
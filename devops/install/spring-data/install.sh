#!/bin/bash
source .env
set -e

echo "Replacing Spring Data version in pom.xml"
POM_FILE="../../../spring-data-tests/pom.xml"
if [ $(uname) = Darwin ]; then
   sed -i '' "s|<spring-data-aerospike.version>.*</spring-data-aerospike.version>|<spring-data-aerospike.version>${SPRING_DATA_VERSION}</spring-data-aerospike.version>|" "$POM_FILE"
   else
      sed -i "s/<spring-data-aerospike.version>.*<\/spring-data-aerospike.version>/<spring-data-aerospike.version>${SPRING_DATA_VERSION}<\/spring-data-aerospike.version>/" "$POM_FILE"
fi

echo "Installing aerospike source"

versionPrefix="${ASDB_VERSION%%.*}"

docker run -d --name aerospike-source -p 3000-3002:3000-3002 \
  -v $PWD/conf/:/etc/aerospike/ --add-host=host.docker.internal:host-gateway \
  $AEROSPIKE_IMAGE_NAME:$ASDB_VERSION --config-file /etc/aerospike/source/V$versionPrefix/aerospike.conf

if [[ "$versionPrefix" -eq 8 ]]; then
  sleep 5
  echo "Add user to source"
  echo "At the first run you should install https://download.aerospike.com/artifacts/aerospike-tools/7.3.1/aerospike-tools-7.3.1-macOS.pkg"
  asadm -U admin -P admin -p 3000 -e 'enable; manage acl create user tester password psw roles truncate sindex-admin user-admin \
  data-admin read-write read write read-write-udf sys-admin udf-admin'
fi

echo "Opening Spring Data project folder"
cd $PATH_TO_SPRING_DATA_PROJECT
echo "Retrieving the latest commit:"
git log --oneline -1
echo "Building Spring Data project"
mvn install -Dgpg.skip -DskipTests
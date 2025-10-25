#!/bin/bash

FEATURES_CONF=""
aerospike_CONF=""

POSITIONAL_ARGS=()

while [[ $# -gt 0 ]]; do
  case $1 in
  --config)
    aerospike_CONF="$2"
    shift
    shift
    ;;
  --features-conf)
    FEATURES_CONF="$2"
    shift
    shift
    ;;
  -* | --*)
    echo "Unknown option $1"
    exit 1
    ;;
  *)
    POSITIONAL_ARGS+=("$1") # save positional arg
    shift                   # past argument
    ;;
  esac
done

set -- "${POSITIONAL_ARGS[@]}"

if [ -z "$aerospike_CONF" ]; then
    echo "Specify aerospike-config"
    exit 1
fi

if [ -z "$FEATURES_CONF" ]; then
    echo "Specify aerospike-config"
    exit 1
fi


mkdir -p "./aerospike/etc/aerospike"
mkdir -p "./aerospike/opt/aerospike/data"
mkdir -p "./aerospike/opt/aerospike/usr/udf/lua"
mkdir -p "./aerospike/opt/data"
mkdir -p "./aerospike/run/aerospike"
touch "./aerospike/run/aerospike/asd.pid"
mkdir -p "./aerospike/usr/udf/lua"
mkdir -p "./aerospike/var/log/aerospike"
touch "./aerospike/var/log/aerospike/aerospike.log"
mkdir -p "./aerospike/var/run/aerospike"
cp "$aerospike_CONF" "./aerospike/etc/aerospike/aerospike.conf"
cp "$FEATURES_CONF" "./aerospike/etc/aerospike/features.conf"

terraform -chdir="./kind" init -no-color
terraform -chdir="./kind" apply -auto-approve -no-color
docker compose -v -f docker-compose.yaml up -d --no-color
terraform -chdir="./k8s" init -no-color
terraform -chdir="./k8s" apply -auto-approve -no-color

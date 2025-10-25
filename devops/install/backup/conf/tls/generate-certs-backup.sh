#!/usr/bin/env bash

# Aerospike Backup Cluster
# Generate self-signed certificates for testing Aerospike with TLS enabled.

PREFIX='backup'
PASSWORD='changeit'
ORG='Aerospike, Inc.'
VALID_DAYS=30
CERT_DIR="./conf/tls/certs/backup"
AEROSPIKE_DIR="./conf/tls/backup"

# create subdirectory to store certs
echo "Creating '${CERT_DIR}' directory"
mkdir -p $CERT_DIR

# create CA cert
echo "Creating CA certificate"
openssl req -x509 -newkey ec:<(openssl ecparam -name prime256v1) -keyout ${CERT_DIR}/${PREFIX}.ca.key -out ${CERT_DIR}/${PREFIX}.ca.crt \
-days ${VALID_DAYS} -subj "/CN=${PREFIX}.ca/O=$ORG/C=US" -extensions v3_ca -passout pass:${PASSWORD}

# create ECDSA certs
CERTS=( "${PREFIX}.client" "${PREFIX}.server" )
for CERT in "${CERTS[@]}"
do
    echo "Creating ${CERT} ECDSA certificate"
    openssl ecparam -genkey -out ${CERT_DIR}/ecparam.pem -name prime256v1
    openssl genpkey -paramfile ${CERT_DIR}/ecparam.pem -out ${CERT_DIR}/${CERT}.key
    openssl req -new -key ${CERT_DIR}/${CERT}.key -out ${CERT_DIR}/${CERT}.csr -subj "/CN=${CERT}/O=$ORG/C=US"
    openssl x509 -req -days ${VALID_DAYS} -passin pass:"$PASSWORD" -in ${CERT_DIR}/${CERT}.csr -CA ${CERT_DIR}/${PREFIX}.ca.crt \
    -CAkey ${CERT_DIR}/${PREFIX}.ca.key -CAcreateserial -out ${CERT_DIR}/${CERT}.crt
    rm ${CERT_DIR}/ecparam.pem ${CERT_DIR}/${CERT}.csr ${CERT_DIR}/${PREFIX}.ca.srl
done

echo "Copying server certificate to aerospike server config directory"
mkdir -p ${AEROSPIKE_DIR}/certs/
mkdir -p ${AEROSPIKE_DIR}/private/
cp ${CERT_DIR}/${PREFIX}.ca.crt ${AEROSPIKE_DIR}/certs/
cp ${CERT_DIR}/${PREFIX}.server.crt ${AEROSPIKE_DIR}/certs/
cp ${CERT_DIR}/${PREFIX}.server.key ${AEROSPIKE_DIR}/private/

echo "---"
tree ${CERT_DIR} ${AEROSPIKE_DIR}/certs/ ${AEROSPIKE_DIR}/private/
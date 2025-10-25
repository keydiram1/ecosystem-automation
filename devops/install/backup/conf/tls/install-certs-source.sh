#!/usr/bin/env bash

# Add certificates to TrustStore/KeyStore for testing Aerospike with TLS enabled
# in JVM-based applications.

PREFIX='source'
CERTS=( "${PREFIX}.client" "${PREFIX}.server" )
CERT_DIR="./conf/tls/certs/source"
TRUSTSTORE_DIR="./conf/tls/etc/pki/certs/source"
TRUSTSTORE_PASSWORD="changeit"
KEYSTORE_DIR="./conf/tls/etc/pki/private/source"
KEYSTORE_PASSWORD="changeit"

# create subdirectory to store certs
echo "Creating TrustStore directory: '${TRUSTSTORE_DIR}'"
mkdir -p $TRUSTSTORE_DIR
echo "Creating KeyStore directory: '${KEYSTORE_DIR}'"
mkdir -p $KEYSTORE_DIR

# create jks variant of CA certificate TrustStore
echo "Creating $TRUSTSTORE_DIR/${PREFIX}.ca.jks"
cp $CERT_DIR/${PREFIX}.ca.crt $TRUSTSTORE_DIR
rm -f $TRUSTSTORE_DIR/${PREFIX}.ca.jks
keytool -importcert -noprompt -storetype jks -alias ${PREFIX}.ca -keystore $TRUSTSTORE_DIR/${PREFIX}.ca.jks -file $CERT_DIR/${PREFIX}.ca.crt -storepass $TRUSTSTORE_PASSWORD

# create pkcs12 variant of client certificate key pair KeyStore
echo "Creating $KEYSTORE_DIR/${PREFIX}.client.p12"
openssl pkcs12 -export -in $CERT_DIR/${PREFIX}.client.crt -inkey $CERT_DIR/${PREFIX}.client.key -out $KEYSTORE_DIR/${PREFIX}.client.chain.p12 -name ${PREFIX}.client -password pass:"$KEYSTORE_PASSWORD" -noiter -nomaciter

# set permission for the .p12 keystore file
chmod 644 $KEYSTORE_DIR/${PREFIX}.client.chain.p12

# print for confirmation
echo "---"
echo "$TRUSTSTORE_DIR/${PREFIX}.ca.jks"
keytool -list -keystore $TRUSTSTORE_DIR/${PREFIX}.ca.jks -storepass $TRUSTSTORE_PASSWORD

echo "---"
echo "$KEYSTORE_DIR/${PREFIX}.client.chain.p12"
keytool -list -keystore $KEYSTORE_DIR/${PREFIX}.client.chain.p12 -storepass $KEYSTORE_PASSWORD

echo "---"
tree $TRUSTSTORE_DIR $KEYSTORE_DIR
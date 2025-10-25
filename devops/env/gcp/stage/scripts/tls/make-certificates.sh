#!/bin/bash -e

mkdir -p "$(pwd)"/{input,output,certs}

echo "Generate Root"
openssl genrsa -out "$(pwd)/output/ca.aerospike.com.key" 2048
openssl req \
-x509 \
-new \
-nodes \
-config "$(pwd)/config/ca.conf" \
-extensions v3_ca \
-key "$(pwd)/output/ca.aerospike.com.key" \
-sha256 \
-days 3650 \
-out "$(pwd)/output/ca.aerospike.com.pem" \
-subj "/C=UK/ST=London/L=London/O=abs/OU=Support/CN=ca.aerospike.com"

echo "Generate Requests & Private Key"
SAN="asd.aerospike.com" openssl req \
-new \
-nodes \
-config "$(pwd)/config/asdb.conf" \
-extensions v3_req \
-out "$(pwd)/input/asd.aerospike.com.req" \
-keyout "$(pwd)/output/asd.aerospike.com.key" \
-subj "/C=UK/ST=London/L=London/O=abs/OU=Server/CN=asd.aerospike.com"

SAN="abs.aerospike.com" openssl req \
-new \
-nodes \
-config "$(pwd)/config/asdb.conf" \
-extensions v3_req \
-out "$(pwd)/input/abs.aerospike.com.req" \
-keyout "$(pwd)/output/abs.aerospike.com.key" \
-subj "/C=UK/ST=London/L=London/O=abs/OU=Server/CN=abs.aerospike.com"

openssl req \
-new \
-nodes \
-out "$(pwd)/input/client.aerospike.com.req" \
-keyout "$(pwd)/output/client.aerospike.com.key" \
-subj "/C=UK/ST=London/L=London/O=abs/OU=Client/CN=client.aerospike.com"

echo "Generate Certificates"
SAN="asd.aerospike.com" openssl x509 \
-req \
-extfile "$(pwd)/config/asdb.conf" \
-in "$(pwd)/input/asd.aerospike.com.req" \
-CA "$(pwd)/output/ca.aerospike.com.pem" \
-CAkey "$(pwd)/output/ca.aerospike.com.key" \
-extensions v3_req \
-days 3649 \
-outform PEM \
-out "$(pwd)/output/asd.aerospike.com.pem" \
-set_serial 110

SAN="abs.aerospike.com" openssl x509 \
-req \
-extfile "$(pwd)/config/asdb.conf" \
-in "$(pwd)/input/abs.aerospike.com.req" \
-CA "$(pwd)/output/ca.aerospike.com.pem" \
-CAkey "$(pwd)/output/ca.aerospike.com.key" \
-extensions v3_req \
-days 3649 \
-outform PEM \
-out "$(pwd)/output/abs.aerospike.com.pem" \
-set_serial 210

openssl x509 \
-req \
-in "$(pwd)/input/client.aerospike.com.req" \
-CA "$(pwd)/output/ca.aerospike.com.pem" \
-CAkey "$(pwd)/output/ca.aerospike.com.key" \
-days 3649 \
-outform PEM \
-out "$(pwd)/output/client.aerospike.com.pem" \
-set_serial 310

echo "Verify Certificate signed by root"
openssl verify \
-verbose \
-CAfile "$(pwd)/output/ca.aerospike.com.pem" \
"$(pwd)/output/asd.aerospike.com.pem"

openssl verify \
-verbose \
-CAfile "$(pwd)/output/ca.aerospike.com.pem" \
"$(pwd)/output/abs.aerospike.com.pem"

openssl verify \
-verbose \
-CAfile "$(pwd)/output/ca.aerospike.com.pem" \
"$(pwd)/output/client.aerospike.com.pem"

echo "Generate JKS trustore"
keytool \
-importcert \
-trustcacerts \
-noprompt \
-storetype jks \
-keystore "$(pwd)/output/ca.aerospike.com.pem.jks" \
-file "$(pwd)/output/ca.aerospike.com.pem" -storepass "password"

echo "Verification JKS trustore"
keytool -list -v -keystore "$(pwd)/output/ca.aerospike.com.pem.jks" -storepass "password"

echo "Generate encryption key"
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 | sed '1d;$d' > "$(pwd)/output/encryption.key.pem"

echo "Generate tester password"
echo -n "psw" > "$(pwd)/output/password.txt"

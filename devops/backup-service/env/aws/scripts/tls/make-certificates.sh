#!/bin/bash -e

## This Script will build a root certificate Authority and generate a series of scripts
## Comment out the generate root piece if you need to make more certs by the same CA

## Note the CN should match the host name for server/client certificates.
## For the buildenv scripts, server,client,ldap and admin are used and are bundled with that install.

mkdir -p "$(pwd)"/rootca 2>/dev/null
rm -rf "$(pwd)"/rootca/* 2>/dev/null
mkdir -p "$(pwd)"/rootca/input
mkdir -p "$(pwd)"/rootca/output

cd "$(pwd)/rootca"

echo "Certificate Authority is here : $(pwd)"
cat <<'EOF' > openssl.conf
HOME                    = .
oid_section             = new_oids

[ new_oids ]
tsa_policy1 = 1.2.3.4.1
tsa_policy2 = 1.2.3.4.5.6
tsa_policy3 = 1.2.3.4.5.7

[ ca ]
default_ca      = CA_default            # The default ca section

[ CA_default ]
dir             = rootca                # Where everything is kept
certs           = $dir/certs            # Where the issued certs are kept
crl_dir         = $dir/crl              # Where the issued crl are kept
database        = $dir/index.txt        # database index file
                                        # several certs with same subject
new_certs_dir   = $dir/newcerts         # default place for new certs

certificate     = $dir/cacert.pem       # The CA certificate
serial          = $dir/serial           # The current serial number
crlnumber       = $dir/crlnumber        # the current crl number
                                        # must be commented out to leave a V1 CRL
crl             = $dir/crl.pem          # The current CRL
private_key     = $dir/private/cakey.pem# The private key
x509_extensions = usr_cert              # The extensions to add to the cert
name_opt        = ca_default            # Subject Name options
cert_opt        = ca_default            # Certificate field options
default_days    = 365                   # how long to certify for
default_crl_days= 30                    # how long before next CRL
default_md      = default               # use public key default MD
preserve        = no                    # keep passed DN ordering
policy          = policy_match

[ policy_match ]
countryName             = match
stateOrProvinceName     = match
organizationName        = match
organizationalUnitName  = optional
commonName              = supplied
emailAddress            = optional

[ policy_anything ]
countryName             = (2 letter code, for example, US)
stateOrProvinceName     = State or Province Name (full name)
localityName            = Locality Name (such as, city)
organizationName        = Organization Name (such as, company)
organizationalUnitName  = Organizational Unit Name (such as, section or department)
commonName              = Common Name (such as, server FQDN or YOUR name)
emailAddress            = joedo@joedocompany.com

[ req ]
default_bits            = 2048
default_keyfile         = privkey.pem
distinguished_name      = req_distinguished_name
attributes              = req_attributes
x509_extensions = v3_ca # The extensions to add to the self signed cert
string_mask = utf8only

[ req_distinguished_name ]
countryName                     = Country Name (2 letter code, for example, US)
countryName_default             = US
countryName_min                 = 2
countryName_max                 = 2
stateOrProvinceName             = State or Province Name (full name)
stateOrProvinceName_default     = Some-State
localityName                    = Locality Name (such as, city)
0.organizationName              = Organization Name (such as, company)
0.organizationName_default      = Your company name
organizationalUnitName          = Organizational Unit Name (such as, section)
commonName                      = Common Name (such as, server FQDN or YOUR name)
commonName_max                  = 64
emailAddress                    = joedo@joedocompany.com
emailAddress_max                = 64

[ req_attributes ]
challengePassword               = A challenge password
challengePassword_min           = 4
challengePassword_max           = 20
unstructuredName                = An optional company name

[ usr_cert ]
basicConstraints=CA:FALSE
nsComment                       = "OpenSSL Generated Certificate"
subjectKeyIdentifier=hash
authorityKeyIdentifier=keyid,issuer


[ v3_req ]
basicConstraints = CA:FALSE
keyUsage = nonRepudiation, digitalSignature, keyEncipherment
subjectAltName = @alt_names

[ v3_ca ]
subjectKeyIdentifier=hash
authorityKeyIdentifier=keyid:always,issuer
basicConstraints = critical,CA:true

[ crl_ext ]
authorityKeyIdentifier=keyid:always

[ proxy_cert_ext ]
basicConstraints=CA:FALSE
nsComment                       = "OpenSSL Generated Certificate"
subjectKeyIdentifier=hash
authorityKeyIdentifier=keyid,issuer
proxyCertInfo=critical,language:id-ppl-anyLanguage,pathlen:3,policy:foo

[ tsa ]
default_tsa = tsa_config1       # the default TSA section

[ tsa_config1 ]
dir             = ./demoCA              # TSA root directory
serial          = $dir/tsaserial        # The current serial number (mandatory)
crypto_device   = builtin               # OpenSSL engine to use for signing
signer_cert     = $dir/tsacert.pem      # The TSA signing certificate
                                        # (optional)
certs           = $dir/cacert.pem       # Certificate chain to include in reply
                                        # (optional)
signer_key      = $dir/private/tsakey.pem # The TSA private key (optional)
signer_digest  = sha256                 # Signing digest to use. (Optional)
default_policy  = tsa_policy1           # Policy if request did not specify it
                                        # (optional)
other_policies  = tsa_policy2, tsa_policy3      # acceptable policies (optional)
digests     = sha1, sha256, sha384, sha512  # Acceptable message digests (mandatory)
accuracy        = secs:1, millisecs:500, microsecs:100  # (optional)
clock_precision_digits  = 0     # number of digits after dot. (optional)
ordering                = yes   # Is ordering defined for timestamps?
                                # (optional, default: no)
tsa_name                = yes   # Must the TSA name be included in the reply?
                                # (optional, default: no)
ess_cert_id_chain       = no    # Must the ESS cert id chain be included?
                                # (optional, default: no)
ess_cert_id_alg         = sha1  # algorithm to compute certificate
                                # identifier (optional, default: sha1)

[ alt_names ]
DNS.1 = ${ENV::SAN}
EOF

echo "Generate Root"
openssl genrsa -out output/rootCA.key 2048
openssl req -x509 -new -nodes -key output/rootCA.key -sha256 -days 3650 -out output/rootCA.pem -subj "/C=UK/ST=London/L=London/O=abs/OU=Support/CN=rootca.abs"

echo
echo "Generate Requests & Private Key"
SAN="server.abs" openssl req -new -nodes -config openssl.conf -extensions v3_req -out input/server.req -keyout output/server.key -subj "/C=UK/ST=London/L=London/O=abs/OU=Server/CN=server.abs"
SAN="client.abs" openssl req -new -nodes -config openssl.conf -extensions v3_req -out input/client.req -keyout output/client.key -subj "/C=UK/ST=London/L=London/O=abs/OU=Client/CN=client.abs"
#openssl req -new -nodes -config openssl.conf -extensions v3_req -out input/admin.req -keyout output/admin.key -subj "/C=UK/ST=London/L=London/O=abs/OU=Admin/CN=admin"
#openssl req -new -nodes -config openssl.conf -extensions v3_req -out input/ldap.req -keyout output/ldap.key -subj "/C=UK/ST=London/L=London/O=abs/OU=Ldap/CN=ldap"
#openssl req -new -nodes -config openssl.conf -extensions v3_req -out input/xdr.req -keyout output/xdr.key -subj "/C=UK/ST=London/L=London/O=abs/OU=XDR/CN=xdr"
#openssl req -new -nodes -config openssl.conf -extensions v3_req -out input/fabric.req -keyout output/fabric.key -subj "/C=UK/ST=London/L=London/O=abs/OU=Fabric/CN=fabric"
#openssl req -new -nodes -config openssl.conf -extensions v3_req -out input/heartbeat.req -keyout output/heartbeat.key -subj "/C=UK/ST=London/L=London/O=abs/OU=Heartbeat/CN=heartbeat"
SAN="agent.abs" openssl req -new -nodes -config openssl.conf -extensions v3_req -out input/agent.req -keyout output/agent.key -subj "/C=UK/ST=London/L=London/O=abs/OU=Agent/CN=agent.abs"
SAN="service.abs" openssl req -new -nodes -config openssl.conf -extensions v3_req -out input/abs.req -keyout output/abs.key -subj "/C=UK/ST=London/L=London/O=abs/OU=Abs/CN=service.abs"

echo
echo "Generate Certificates"
SAN="server.abs" openssl x509 -req -extfile openssl.conf -in input/server.req -CA output/rootCA.pem   -CAkey output/rootCA.key  -extensions v3_req -days 3649 -outform PEM -out output/server.pem -set_serial 110
SAN="client.abs" openssl x509 -req -extfile openssl.conf -in input/client.req -CA output/rootCA.pem   -CAkey output/rootCA.key  -extensions v3_req -days 3649 -outform PEM -out output/client.pem -set_serial 210
#openssl x509 -req -extfile openssl.conf -in input/admin.req -CA output/rootCA.pem   -CAkey output/rootCA.key  -extensions v3_req -days 3649 -outform PEM -out output/admin.pem -set_serial 310
#openssl x509 -req -extfile openssl.conf -in input/ldap.req -CA output/rootCA.pem   -CAkey output/rootCA.key  -extensions v3_req -days 3649 -outform PEM -out output/ldap.pem -set_serial 410
#openssl x509 -req -extfile openssl.conf -in input/xdr.req -CA output/rootCA.pem   -CAkey output/rootCA.key  -extensions v3_req -days 3649 -outform PEM -out output/xdr.pem -set_serial 510
#openssl x509 -req -extfile openssl.conf -in input/fabric.req -CA output/rootCA.pem   -CAkey output/rootCA.key  -extensions v3_req -days 3649 -outform PEM -out output/fabric.pem -set_serial 610
#openssl x509 -req -extfile openssl.conf -in input/heartbeat.req -CA output/rootCA.pem   -CAkey output/rootCA.key  -extensions v3_req -days 3649 -outform PEM -out output/heartbeat.pem -set_serial 710
SAN="agent.abs" openssl x509 -req -extfile openssl.conf -in input/agent.req -CA output/rootCA.pem   -CAkey output/rootCA.key  -extensions v3_req -days 3649 -outform PEM -out output/agent.pem -set_serial 810
SAN="service.abs" openssl x509 -req -extfile openssl.conf -in input/abs.req -CA output/rootCA.pem   -CAkey output/rootCA.key  -extensions v3_req -days 3649 -outform PEM -out output/abs.pem -set_serial 910

echo
echo "Verify Certificate signed by root"
openssl verify -verbose -CAfile output/rootCA.pem output/server.pem
openssl verify -verbose -CAfile output/rootCA.pem output/client.pem
#openssl verify -verbose -CAfile output/rootCA.pem output/admin.pem
#openssl verify -verbose -CAfile output/rootCA.pem output/ldap.pem
#openssl verify -verbose -CAfile output/rootCA.pem output/xdr.pem
#openssl verify -verbose -CAfile output/rootCA.pem output/fabric.pem
#openssl verify -verbose -CAfile output/rootCA.pem output/heartbeat.pem
openssl verify -verbose -CAfile output/rootCA.pem output/agent.pem
openssl verify -verbose -CAfile output/rootCA.pem output/abs.pem

echo "Generate encryption key"
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 | sed '1d;$d' > output/encryption_key.pem

echo "Generate tester password"
echo -n "psw" > output/password.txt

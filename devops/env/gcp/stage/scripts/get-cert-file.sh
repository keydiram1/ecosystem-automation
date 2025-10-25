#!/bin/bash

find . -maxdepth 1 -type f \( -name "ca.aerospike.com.pem" \) -exec rm -f {} \;
gcloud secrets versions access latest --secret="ca-aerospike-com-pem" --project=ecosystem-connectors-data > ca.aerospike.com.pem

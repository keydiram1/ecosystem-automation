#!/bin/bash

#  nohup ./load-data.sh > load-data-output.log 2>&1 &

# ========== Configuration ==========
USER="tester"
PASSWORD="psw"
#NAMESPACE="source-ns1"
HOSTS="asd.ws-2.ecosys.internal"
PORT=4333
THREADS=40
BATCH_SIZE=100
WORKLOAD="I"
#START_KEY=0
#KEYS=25000000
THROUGHPUT=4000
WRITE_SOCKET_TIMEOUT=50000
MAX_RETRIES=5
SLEEP_BETWEEN_RETRIES=3
WRITE_TIMEOUT=5000
SLEEP_BETWEEN_TESTS=180 # 3 minutes

# Timestamp for log files
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

# ========== Run asbench Function ==========
run_asbench() {
	SET_NAME="$1"
	BIN_SPEC="$2"
	START_KEY="$3"
  KEYS="$4"
  NAMESPACE="$5"
	LOG_FILE="load_data_${SET_NAME}_${TIMESTAMP}.log"

	echo "Starting test for set: $SET_NAME at $(date) with threads: $THREADS"

	asbench \
		--user "$USER" \
		--password "$PASSWORD" \
		--namespace "$NAMESPACE" \
		--hosts "$HOSTS:asd.aerospike.com:$PORT" \
		--tls-cafile ca.aerospike.com.pem \
		--tls-enable \
		--set "$SET_NAME" \
		-o "$BIN_SPEC" \
		--threads "$THREADS" \
		--batch-size "$BATCH_SIZE" \
		--workload "$WORKLOAD" \
		--throughput "$THROUGHPUT" \
		--start-key "$START_KEY" \
		--keys "$KEYS" \
		--write-timeout "$WRITE_TIMEOUT" \
		--write-socket-timeout "$WRITE_SOCKET_TIMEOUT" \
		--max-retries "$MAX_RETRIES" \
		--sleep-between-retries "$SLEEP_BETWEEN_RETRIES" \
		--debug | tee "$LOG_FILE"

	echo "Completed test for set: $SET_NAME at $(date)"
	echo "---------------------------------------------"

	echo "Sleeping for $(($SLEEP_BETWEEN_TESTS / 60)) minutes before the next test..."
#	sleep "$SLEEP_BETWEEN_TESTS"
}

# ========== Run Data Types ==========

# perf test
#run_asbench "SetPerformanceTest" "B1024"

run_asbench "test_set_1" "B1000" "0" "100000000" "source-ns1"
run_asbench "test_set_2" "B2000" "0" "100000000" "source-ns1"
run_asbench "test_set_3" "B2000" "0" "100000000" "source-ns2"
run_asbench "test_set_4" "B3000" "0" "100000000" "source-ns2"

#run_asbench "mixed_set_1" "B1000" "0" "25000000" "source-ns1"
#run_asbench "mixed_set_1" "B2000" "25000000" "25000000" "source-ns1"
#run_asbench "mixed_set_1" "B2000" "50000000" "25000000" "source-ns1"
#run_asbench "mixed_set_1" "B3000" "75000000" "25000000" "source-ns1"
#
#run_asbench "mixed_set_2" "B2000" "0" "25000000" "source-ns1"
#run_asbench "mixed_set_2" "B1000" "25000000" "25000000" "source-ns1"
#run_asbench "mixed_set_2" "B2000" "50000000" "25000000" "source-ns1"
#run_asbench "mixed_set_2" "B3000" "75000000" "25000000" "source-ns1"
#
#run_asbench "mixed_set_3" "B2000" "0" "25000000" "source-ns2"
#run_asbench "mixed_set_3" "B2000" "25000000" "25000000" "source-ns2"
#run_asbench "mixed_set_3" "B1000" "50000000" "25000000" "source-ns2"
#run_asbench "mixed_set_3" "B3000" "75000000" "25000000" "source-ns2"
#
#run_asbench "mixed_set_4" "B3000" "0" "25000000" "source-ns2"
#run_asbench "mixed_set_4" "B2000" "25000000" "25000000" "source-ns2"
#run_asbench "mixed_set_4" "B1000" "50000000" "25000000" "source-ns2"
#run_asbench "mixed_set_4" "B2000" "75000000" "25000000" "source-ns2"



### BLOBs
#run_asbench "blob_small" "B400"
#run_asbench "blob_large" "B2000"
##
### Scalars
#run_asbench "scalars_small" "S100,I4,S100,I4,S100"
#run_asbench "scalars_large" "S1000,S1000,S1000"
##
### Lists
#run_asbench "list_small" "[50*I1]"
#run_asbench "list_large" "[300*I1]"
##
### Maps
#run_asbench "map_small" "{20*S5:I1}"
#run_asbench "map_large" "{100*S10:I2}"
#
echo "All asbench insertions completed successfully!"
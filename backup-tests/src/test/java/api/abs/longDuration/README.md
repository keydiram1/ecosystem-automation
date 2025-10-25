# Long Duration Test for ABS (Aerospike Backup Service)

## Overview

This test evaluates the Aerospike Backup Service (ABS) under long-duration and high-load conditions. The goal is to validate stability, reliability, and error handling during extended operation periods. The test environment uses a 3-node Aerospike cluster.

---

## Test Scenarios

### Test Details:

- **Job Name:** `test-abs-long-duration-aws`
- **Test Duration:** 25 hours
- **Test Setup:**
  - **Five Test Classes:**
    - Each class runs in parallel, executing data creation, backup, and restore in a loop for 25 hours.
    - Each class processes approximately 13.5 million records during the test, resulting in a total of ~67.5 million records across all classes.
  - **Incremental Backup Class:**
    - Runs in a loop with a small amount of data, continuously performing backup and restore operations.
  - **PrintLogsClass:**
    - Monitors the system for errors in both the Aerospike cluster and the backup service.
    - Collects and logs errors from the test classes, reporting the specific iteration where errors occurred, if any.
    - Prints the memory and CPU usage of the backup service every few minutes.
  - **Asbench Configuration:**
    - Configurable parameters:
      - `on_going_throughput`: Specifies the throughput rate for data creation.
      - `data_spikes_duration`: Defines the duration of data spikes during the test.
    - **V3 Configuration:**
      - `on_going_throughput=50`
      - `data_spikes_duration=5`

---

## Results

### AWS Environment

#### V3 Results:

- **Five Test Classes:**
  - Completed 25 hours of testing without errors.
  - Processed a total of ~67.5 million records.
- **Incremental Backup Class:**
  - Completed 25 hours of testing without errors.
- **PrintLogsClass:**
  - No errors detected from the backup service, Aerospike cluster, or test classes.
  - Memory and CPU usage of the backup service remained stable with no leaks.

#### V4 Results:

- To be filled

### GCP Environment

#### V3 Results:

- To be filled

#### V4 Results:

- To be filled

---

## Instructions

This test must run in the cloud using the `test-abs-long-duration-aws` Jenkins job.

---


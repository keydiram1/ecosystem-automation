# Load Test for ABS (Aerospike Backup Service)

## Overview

This test is designed to assess the performance and reliability of Aerospike Backup Service (ABS) under load. It evaluates backup and restore operations for large datasets, running multiple test scenarios in parallel. The test environment uses a 3-node Aerospike cluster with a replication factor of 2, effectively doubling the data operations.

---

## Test Scenarios

### Test Details:
- **AWS Environment:**
  - V2: Backup and restore of 12 million records.
  - V3: Backup and restore of 15 million records.
- **GCP Environment:**
  - V3: Backup and restore of 16 million records.
- **V4:** Results for both AWS and GCP will be added later.

### Test Workflow:
1. Full backups of 15M records.
2. Full restores of 15M records.
3. Incremental backup and restore of a single record, executed in a loop.

---

## Results

### AWS Environment

#### V2 Results (12M Records):
- **Backup:** 1 minute 30 seconds
- **Restore:** 2 minutes 30 seconds
- **Total Backup and Restore:** 24 million records

#### V3 Results (15M Records):
- **Backup:** 2 minutes 18 seconds
- **Restore:** 2 minutes 36 seconds
- **Total Backup and Restore:** 30 million records

#### V4 Results:
- To be filled

### GCP Environment

#### V3 Results (16M Records):
- **Backup:**
  - Class 1: 2 minutes 36 seconds
  - Class 2: 2 minutes 31 seconds
  - Class 3: 1 minute 39 seconds
  - **Total Backup:** 48 million records across 3 classes
- **Restore:**
  - Class 1 (16M records): 1 minute 24 seconds
  - Class 2 (16M records): 1 minute 24 seconds
  - Class 3 (15M records): 1 minute 24 seconds
  - **Total Restore:** 47 million records across 3 classes
- **Incremental and Full Restore Loops:**
  - **Incremental Restore:** Completed ~4 iterations without errors.
  - **Full Restore:** Completed ~60 iterations without errors.

#### V4 Results:
- To be filled

---

## Instructions

This test must run in the cloud using the `test-abs-load-aws` and `Test-ABS-On-GCP` Jenkins jobs.

---

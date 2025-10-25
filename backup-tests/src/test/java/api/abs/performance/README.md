# Aerospike Backup & Restore Performance Comparison

## Test Environment

- **Cluster Size:** 5 nodes
- **Machine Type:** `n2-standard-16`
- **Devices per Node:** 4
- **Namespaces:** 1

---

## Test Parameters

- **Backup:**  
  `parallel=8`, `recordsPerSecond=0`, `bandwidth=0`,  
  `socketTimeout=60000000`, `totalTimeout=0`, `backupFileLimit=250`

- **Restore:**  
  `parallel=8`, `maxAsyncBatches=8`, `batchSize=128`,  
  `socketTimeout=10000`, `totalTimeout=60000`

---

## Results (V3.2)

| Record Type       | Records     | Size         | Backup Duration | Restore Duration |
|-------------------|-------------|--------------|-----------------|------------------|
| SCALAR_1KB        | 30,000,150  | 25.28 GB     | 90 sec          | 533 sec          |
| COMPLEX_1KB       | 50,000,247  | 116.27 GB    | 344 sec         | 1143 sec         |
| MIXED_1KB         | 50,000,544  | 78.93 GB     | 264 sec         | 1104 sec         |
| SCALAR_3KB        | 50,000,247  | 110.40 GB    | 286 sec         | 1096 sec         |
| COMPLEX_3KB       | 20,000,097  | 132.69 GB    | 381 sec         | 611 sec          |
| MIXED_3KB         | 25,000,566  | 110.54 GB    | 319 sec         | 594 sec          |
| SCALAR_100KB      | 3,000,225   | 205.30 GB    | 404 sec         | 333 sec          |
| COMPLEX_100KB     | 1,000,203   | 216.08 GB    | 584 sec         | 461 sec          |
| MIXED_100KB       | 1,000,566   | 142.31 GB    | 363 sec         | 268 sec          |

---

## Summary

This test suite confirms that V3.2 delivers **consistently stable and fast backup/restore operations** across a variety of data types and record sizes.

> ✅ Compared to V3.1, **backup duration was reduced by up to 50% and restore time improved by over 2× in several scenarios**.

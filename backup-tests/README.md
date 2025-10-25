# Backup Tests

## ABS (Aerospike Backup Service)

### Install ABS Locally and Run Tests:

1. Edit the ABS installation configuration in `devops/install/abs/.env`.
2. Install the ABS by executing
```bash
cd ../devops/install/abs
./install.sh
```
3. Run a single test class. for example: `RestoreFullBackupTest`.
4. Run all tests in parallel with the following command:
```bash
mvn integration-test -Dgroups=ABS-E2E -Djunit.jupiter.execution.parallel.config.fixed.parallelism=30
```
5. Inspect and investigate the test results at [Report Portal Test Results](http://3.65.168.247:8080/ui/#pitr/launches/all).
6. Uninstall the ABS by executing
```bash
cd ../devops/install/abs
./uninstall.sh
```

### Install ABS in Jenkins Slave and Run Tests:

1. Navigate to the Jenkins job [Test ABS Local](https://3.122.67.127/view/ABS-Tests-Local/job/test-abs-local/job/master/).
2. Click on the "Build with Parameters" button and build.
3. Inspect and investigate the test results at [Report Portal Test Results](http://3.65.168.247:8080/ui/#pitr/launches/all).

### Install ABS in AWS and Run Tests:

1. Execute the Jenkins job [Test ABS AWS](https://3.122.67.127/view/ABS-Test-AWS/job/test-abs-aws/job/master/).
2. Click on the "Build with Parameters" button and build.
3. View and analyze the test results at [Report Portal Test Results](http://3.65.168.247:8080/ui/#pitr/launches/all).


## ADR (Aerospike Disaster Recovery)

### Install ADR Locally and Run Tests:

1. Edit the ADR installation configuration in `devops/install/backup/.env`.
2. Install the ADR by executing
```bash
cd ../devops/install/backup
./install.sh
```
3. Run a single test class. for example: `RestoreTest`.
4. Run all tests in parallel with the following command:
```bash
mvn integration-test -Dgroups=ADR-E2E -Djunit.jupiter.execution.parallel.config.fixed.parallelism=30
```
5. Inspect and investigate the test results at [Report Portal Test Results](http://3.65.168.247:8080/ui/#pitr/launches/all).
6. Uninstall the ADR by executing
```bash
cd ../devops/install/backup
./uninstall.sh
```

### Install ADR in Jenkins Slave and Run Tests:

1. Navigate to the Jenkins job [Test ADR Local](https://3.122.67.127/view/ADR-Tests%20-%20Local/job/test-adr-local/).
2. Click on the "Build with Parameters" button and build.
3. Inspect and investigate the test results at [Report Portal Test Results](http://3.65.168.247:8080/ui/#pitr/launches/all).

### Install ADR in AWS and Run Tests:

1. Execute the Jenkins job [Test ADR AWS](https://3.122.67.127/view/ADR-Tests%20-%20AWS/job/test-adr-aws/job/master/).
2. Click on the "Build with Parameters" button and build.
3. View and analyze the test results at [Report Portal Test Results](http://3.65.168.247:8080/ui/#pitr/launches/all).
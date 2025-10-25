# Spring Data Tests

### Install Spring Data Locally and Run Tests:

1. Edit the Spring Data installation configuration in `devops/install/spring-data/.env`.
2. Install the Spring Data module by executing:
    ```bash
    cd ../devops/install/spring-data
    ./install.sh
    ```
3. Run a single test class, for example: `AerospikeTemplateExecuteTests`.
4. Run all tests in parallel with the following command:
    ```bash
    mvn integration-test -Dgroups=SPRING-DATA-TESTS -Djunit.jupiter.execution.parallel.config.fixed.parallelism=5
    ```
5. Inspect and investigate the test results at [Report Portal Test Results](http://3.65.168.247:8080/ui/#spring_data/launches/all).
6. Uninstall the Spring Data module by executing:
    ```bash
    cd ../devops/install/spring-data
    ./uninstall.sh
    ```

### Install Spring Data in Jenkins Slave and Run Tests:

1. Navigate to the Jenkins job [Test Spring Data Local](https://3.122.67.127/view/Spring-Data/job/test-spring-data-local/).
2. Click on the "Build with Parameters" button and build.
3. Inspect and investigate the test results at [Report Portal Test Results](http://3.65.168.247:8080/ui/#spring_data/launches/all).

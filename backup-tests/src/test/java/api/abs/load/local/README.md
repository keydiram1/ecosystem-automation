## Load Test with Two Clusters

To run the load test with two clusters, follow these steps:

1. Open the **devops/install/abs/.env** file and set `INSTALL_TWO_SOURCE_CLUSTERS` to `true`.
2. Set `SERVICE_CONF_FILE` to `configLoad.yml` in the `.env` file.
3. Run the **devops/install/abs/install.sh** script
4. Run all load classes in parallel with the following command:
```bash
cd ../../../../../../..
pwd
mvn integration-test -Dgroups=ABS-LOCAL-LOAD-TEST -Djunit.jupiter.execution.parallel.config.fixed.parallelism=10
```
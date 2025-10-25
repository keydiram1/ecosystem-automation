# Ecosystem Automated Tests

This project contains automated tests for various Aerospike components. It is divided into two main modules:

- **backup-tests**: This module tests the Aerospike Backup Service (ABS) and Aerospike Disaster Recovery (ADR) processes.
- **spring-data-tests**: This module tests the integration and functionality of Aerospike Spring Data.

## Modules Overview

### backup-tests
The `backup-tests` module focuses on validating the reliability and efficiency of ABS and ADR in various scenarios. It ensures that data can be backed up and restored accurately, and that disaster recovery processes function as expected.

### spring-data-tests
The `spring-data-tests` module tests the integration of Aerospike with Spring Data, ensuring compatibility and functionality within the Spring ecosystem. It verifies that the Spring Data repository interfaces work seamlessly with Aerospike.

## Getting Started

To get started, please refer to the specific `README.md` files located in each module directory for detailed instructions on testing procedures and environment setup.

- [backup-tests/README.md](backup-tests/README.md)
- [spring-data-tests/README.md](spring-data-tests/README.md)


package api.backup.longtime;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Tag("ADR-LONG-TIME-TEST")
@Execution(ExecutionMode.CONCURRENT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings("java:S2699")
class LongTimeBackup3Test extends LongTimeBackup {

    @BeforeAll
    void setUp() {
        SOURCE_NAMESPACE = "source-ns3";
        BACKUP_NAMESPACE = "adr-ns3";
        SOURCE_CLUSTER_NAME = "SourceClusterLongTimeBackup3Test";
        BACKUP_NAME = "StressBackup3TestBackupName";
        POLICY_NAME = "StressBackup3TestPolicy";
        SET_NAME = "setStressBackup3Test";
        setUpParent();
    }

    @Test
    void onGoingDataCreation() {
        onGoingDataCreationParent();
    }

    @Test
    void printProgress() {
        printProgressParent();
    }
}

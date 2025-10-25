package api.abs.load.aws.backup;

import api.abs.AbsBackupApi;
import api.abs.AbsRoutineApi;
import api.abs.load.aws.LoadRunner;
import utils.AerospikeLogger;
import utils.ConfigParametersHandler;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.abs.AerospikeDataUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;


class LoadBackupParent extends LoadRunner {
    protected String routineName;
    private int numberOfRecordsInDB;

    protected void setUpParent() {
        minutesToWaitForAllLoadClassesToFinishSetup = LOAD_LEVEL.equals("low") ? 5 : 60;
        String sourceNamespace = AbsRoutineApi.getAnyNamespaceForRoutine(routineName);

        AerospikeDataUtils.truncateSourceNamespace(sourceNamespace);

        createBigData(sourceNamespace);
        numberOfRecordsInDB = AerospikeCountUtils.getSetObjectCount(srcClient, SET, sourceNamespace);
        assertThat(numberOfRecordsInDB).isGreaterThan(1000);

        waitGroup.wait(minutesToWaitForAllLoadClassesToFinishSetup);
    }

    protected void testBackupParent() {
        int timeout = ConfigParametersHandler.getParameter("LOAD_LEVEL").equals("low") ? 2 : 4;

        long backupStartTime = System.currentTimeMillis();
        var backupKey = AbsBackupApi.startFullBackupSync(routineName, Duration.ofMinutes(timeout));

        long backupDuration = System.currentTimeMillis() - backupStartTime;
        assertThat(backupDuration).isLessThan(560_000);

        AerospikeLogger.info("The number of records in the backup is: " + numberOfRecordsInDB);
        assertThat(backupKey.getRecordCount()).isEqualTo(numberOfRecordsInDB);
    }
}

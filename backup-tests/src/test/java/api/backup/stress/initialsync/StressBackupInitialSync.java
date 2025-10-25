package api.backup.stress.initialsync;

import api.backup.BackupManager;
import api.backup.stress.StressRunner;
import utils.ASBench;
import utils.AerospikeLogger;
import utils.AutoUtils;
import utils.ConfigParametersHandler;
import utils.aerospike.AerospikeCountUtils;

import java.util.ArrayList;
import java.util.Collections;

public class StressBackupInitialSync extends StressRunner {

    void setUpParent() {
        DC_NAME = "AdrDC";
        setPerformanceVariables();
        AerospikeLogger.info("asbench duration in seconds: " + asBenchDurationInSeconds);
        AerospikeLogger.info("Seconds to wait for backup: " + waitForBackupLoopCount * secondsToSleepInBackupLoop);
        AerospikeLogger.info("Minutes to wait for all stress classes to finish setup: "
                + minutesToWaitForAllStressClassesToFinishSetup);

        BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
        AutoUtils.sleep(20_000);
        ASBench.on(SOURCE_NAMESPACE, SET_NAME).keys(100000000).duration(asBenchDurationInSeconds).run();

        AerospikeLogger.info("Records in backup before waiting for all classes: "
                + AerospikeCountUtils.getSetObjectCount(backupClient, SET_NAME, BACKUP_NAMESPACE));

        waitGroup.wait(minutesToWaitForAllStressClassesToFinishSetup); // wait for all tests to finish setup
    }

    void createBackupWithInitialSync0Parent() {
        AerospikeLogger.info("Records in backup before creating enabled backup: "
                + AerospikeCountUtils.getSetObjectCount(backupClient, SET_NAME, BACKUP_NAMESPACE));
        BackupManager.createEnabledBackup(BACKUP_NAME, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, BACKUP_NAMESPACE, POLICY_NAME,
                DC_NAME, new ArrayList<>(Collections.singletonList(SET_NAME)));

        waitForBackup();
    }

    private void setPerformanceVariables() {
        asBenchDurationInSeconds = 1;
        waitForBackupLoopCount = 30;
        if (ConfigParametersHandler.getParameter("asbench_duration_seconds") != null) {
            if (System.getProperty("asbench_duration_seconds").equals("200")) {
                asBenchDurationInSeconds = 200;
                waitForBackupLoopCount = 300;
                minutesToWaitForAllStressClassesToFinishSetup = 30;
            }
        }
    }
}

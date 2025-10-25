package api.backup.stress.backup;

import api.backup.BackupManager;
import api.backup.stress.StressRunner;
import utils.ASBench;
import utils.AerospikeLogger;
import utils.AutoUtils;
import utils.ConfigParametersHandler;

import java.util.ArrayList;
import java.util.Collections;

public class StressBackup extends StressRunner {

    void setUpParent() {
        DC_NAME = "StressBackupDC";
        setPerformanceVariables();
        AerospikeLogger.info("asbench duration in seconds: " + asBenchDurationInSeconds);
        AerospikeLogger.info("Seconds to wait for backup: " + waitForBackupLoopCount * secondsToSleepInBackupLoop);
        AerospikeLogger.info("Minutes to wait for all stress classes to finish setup: "
                + minutesToWaitForAllStressClassesToFinishSetup);

        BackupManager.cleanUp(BACKUP_NAMESPACE, SOURCE_NAMESPACE, SOURCE_CLUSTER_NAME, BACKUP_NAME, POLICY_NAME);
        AutoUtils.sleep(20_000);

        BackupManager.createEnabledBackup(BACKUP_NAME, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, BACKUP_NAMESPACE, POLICY_NAME,
                DC_NAME, new ArrayList<>(Collections.singletonList(SET_NAME)));

        waitGroup.wait(minutesToWaitForAllStressClassesToFinishSetup); // wait for all tests to finish setup
    }

    void createBackupParent() {
        int asBenchLoopCount = asBenchDurationInSeconds == 200 ? 40 : 5;
        for (int i = 0; i < asBenchLoopCount; i++) {
            ASBench.on(SOURCE_NAMESPACE, SET_NAME).keys(100000000).duration(1).run();
            AutoUtils.sleep(1_000);
        }

        waitForBackup();
    }

    private void setPerformanceVariables() {
        asBenchDurationInSeconds = 40;
        waitForBackupLoopCount = 15;
        if (ConfigParametersHandler.getParameter("asbench_duration_seconds") != null) {
            if (ConfigParametersHandler.getParameter("asbench_duration_seconds").equals("200")) {
                asBenchDurationInSeconds = 200;
                waitForBackupLoopCount = 300;
                minutesToWaitForAllStressClassesToFinishSetup = 30;
            }
        }
    }
}

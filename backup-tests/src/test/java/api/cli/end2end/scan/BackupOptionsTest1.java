package api.cli.end2end.scan;

import api.cli.BackupProcessException;
import api.cli.BackupResult;
import api.cli.CliBackup;
import api.cli.CliRestore;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.ASBench;
import utils.AerospikeLogger;
import utils.AutoUtils;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.abs.AerospikeDataUtils;
import utils.cliBackup.CliBackupRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("CLI-BACKUP")
class BackupOptionsTest1 extends CliBackupRunner {
    private static final String SOURCE_NAMESPACE = "source-ns6";
    private static final String SET1 = "AsetBackupOptions";
    private static int numberOfRecordsToBackup;
    private static Key KEY1;
    private static final String STRING_BIN = "testBin";
    private static final String BACKUP_DIR = "BackupOptions1Dir";

    @BeforeAll
    static void setUp() {
        KEY1 = new Key(SOURCE_NAMESPACE, SET1, "IT1");
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        ASBench.on(SOURCE_NAMESPACE, SET1).keys(10).run();
        numberOfRecordsToBackup = AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE);
        assertThat(numberOfRecordsToBackup).isEqualTo(10);
    }

    @Test
    void recordsPerSecond() {
        long startTime = System.currentTimeMillis();
        CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR).setRecordsPerSecond(1).run();
        long duration = System.currentTimeMillis() - startTime;
        AerospikeLogger.info("Backup for " + numberOfRecordsToBackup + " records took " + duration + " milliseconds");
        assertThat(duration / 1000).isGreaterThan(8).isLessThan(12);
    }

    @Test
    void totalTimeout() {
        assertThatThrownBy(() -> {
            CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR).setRecordsPerSecond(1).setTotalTimeout(1).run();
        }).isInstanceOf(BackupProcessException.class)
                .hasMessageContaining("command execution timed out on client");
    }

    @Test
    void socketTimeout() {
        assertThatThrownBy(() -> {
            CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR).setRecordsPerSecond(1).setSocketTimeout(1).run();
        }).isInstanceOf(BackupProcessException.class)
                .hasMessageContaining("ResultCode: TIMEOUT");
    }

    @Test
    void fileLimit2() {
        int fileLimit = 1;

        BackupResult backupResult = CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR).setFileLimit(fileLimit).run();
        assertThat(backupResult.getFilesWritten()).isGreaterThan(8).isLessThan(15);

        backupResult = CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR).run();
        assertThat(backupResult.getFilesWritten()).isLessThan(9);
    }

    @Test
    void outputFile() {
        String firstValueCreate = "firstValueCreate" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, STRING_BIN, firstValueCreate);

        String fileNameForBackup = "fileNameForBackup";
        BackupResult backupResult = CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR, 1).setOutputFile(fileNameForBackup).run();

        String fileNameInDirectory = AutoUtils.getFileNameFromDir(backupResult.getBackupDir());

        assertThat(fileNameInDirectory).isEqualTo(fileNameForBackup + ".asb");

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        CliRestore.on(SOURCE_NAMESPACE, backupResult.getBackupDir()).run();

        Record retrievedRecord = srcClient.get(null, KEY1);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(firstValueCreate);
    }

    @Test
    void removeFile() {
        String firstValueCreate = "firstValueCreate" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, STRING_BIN, firstValueCreate);

        CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR).run();
        assertThatThrownBy(() -> {
            CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR).run(false, true);
        }).isInstanceOf(BackupProcessException.class)
                .hasMessageContaining("backup folder must be empty");

        BackupResult backupResult = CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR).setRemoveFiles().run(false, true);

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        CliRestore.on(SOURCE_NAMESPACE, backupResult.getBackupDir()).run();

        Record retrievedRecord = srcClient.get(null, KEY1);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(firstValueCreate);
    }

    @Test
    void removeArtifacts() {
        BackupResult backupResult = CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR).run();
        String backupDir = backupResult.getBackupDir();
        long numberOfFilesBeforeRemoveArtifacts = AutoUtils.countFilesInDir(backupDir);
        assertThat(numberOfFilesBeforeRemoveArtifacts).isEqualTo(CliBackup.DEFAULT_PARALLEL);

        CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR).setRemoveArtifacts().run(false, false);
        long numberOfFilesAfterRemoveArtifacts = AutoUtils.countFilesInDir(backupDir);
        assertThat(numberOfFilesAfterRemoveArtifacts).isEqualTo(0);
    }

    @Test
    void parallel() {
        BackupResult backupResult = CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR, 3).run();
        String backupDir = backupResult.getBackupDir();
        long numberOfFilesBeforeRemoveArtifacts = AutoUtils.countFilesInDir(backupDir);
        assertThat(numberOfFilesBeforeRemoveArtifacts).isEqualTo(3);

        backupResult = CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR, 7).run();
        backupDir = backupResult.getBackupDir();
        numberOfFilesBeforeRemoveArtifacts = AutoUtils.countFilesInDir(backupDir);
        assertThat(numberOfFilesBeforeRemoveArtifacts).isEqualTo(7);
    }

    @Test
    void modifiedAfter() {
        String valueBefore = "valueBefore" + System.currentTimeMillis();
        String valueAfter = "valueAfter" + System.currentTimeMillis();

        AerospikeDataUtils.put(KEY1, STRING_BIN, valueBefore);
        AerospikeDataUtils.put(KEY1, STRING_BIN, valueBefore + " updated");

        AutoUtils.sleep(2000);

        String modifiedAfterTime = AutoUtils.getCurrentFormattedTime();

        AutoUtils.sleep(2000);

        Key afterKey = new Key(SOURCE_NAMESPACE, SET1, "IT2");
        AerospikeDataUtils.put(afterKey, STRING_BIN, valueAfter);
        AerospikeDataUtils.put(afterKey, STRING_BIN, valueAfter + " updated");

        BackupResult modifiedAfterBackupResult = CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR).setModifiedAfter(modifiedAfterTime).run();

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        CliRestore.on(SOURCE_NAMESPACE, modifiedAfterBackupResult.getBackupDir()).run();

        // Verify only the second record (after the specified time) is restored
        Record retrievedAfterRecord = srcClient.get(null, afterKey);
        assertThat(retrievedAfterRecord).isNotNull();
        assertThat(retrievedAfterRecord.getString(STRING_BIN)).isEqualTo(valueAfter + " updated");

        // Ensure the first record is not restored
        Record retrievedBeforeRecord = srcClient.get(null, KEY1);
        assertThat(retrievedBeforeRecord).isNull();
    }

    @Test
    void modifiedBefore() {
        String valueBefore = "valueBefore" + System.currentTimeMillis();
        String valueAfter = "valueAfter" + System.currentTimeMillis();

        AerospikeDataUtils.put(KEY1, STRING_BIN, valueBefore);
        AerospikeDataUtils.put(KEY1, STRING_BIN, valueBefore + " updated");

        AutoUtils.sleep(2000);

        String modifiedBeforeTime = AutoUtils.getCurrentFormattedTime();
        AerospikeLogger.info("modifiedBeforeTime=" + modifiedBeforeTime);

        AutoUtils.sleep(2000);

        Key afterKey = new Key(SOURCE_NAMESPACE, SET1, "IT2");
        AerospikeDataUtils.put(afterKey, STRING_BIN, valueAfter);
        AerospikeLogger.info("Updating the  afterKey at " + AutoUtils.getCurrentFormattedTime());
        AerospikeDataUtils.put(afterKey, STRING_BIN, valueAfter + " updated");

        BackupResult modifiedBeforeBackupResult = CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR).setModifiedBefore(modifiedBeforeTime).run();

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        CliRestore.on(SOURCE_NAMESPACE, modifiedBeforeBackupResult.getBackupDir()).run();

        // Verify only the first record (before the specified time) is restored
        Record retrievedBeforeRecord = srcClient.get(null, KEY1);
        assertThat(retrievedBeforeRecord).isNotNull();
        assertThat(retrievedBeforeRecord.getString(STRING_BIN)).isEqualTo(valueBefore + " updated");

        // Ensure the second record is not restored
        Record retrievedAfterRecord = srcClient.get(null, afterKey);
        assertThat(retrievedAfterRecord).isNull();
    }
}
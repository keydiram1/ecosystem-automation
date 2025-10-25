package api.cli.end2end.scan;

import api.cli.*;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.policy.WritePolicy;
import org.junit.jupiter.api.*;
import utils.ASBench;
import utils.AerospikeLogger;
import utils.AutoUtils;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.abs.AerospikeDataUtils;
import utils.cliBackup.CliBackupRunner;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("CLI-BACKUP")
class RestoreOptionsTest extends CliBackupRunner {
    private static final String SOURCE_NAMESPACE = "source-ns9";
    private static final String SET1 = "setBackupOptions";
    private static Key KEY1;
    private static final String BACKUP_DIR = "RestoreOptionsDir";
    private static int numberOfRecordsToBackup;
    private static final String STRING_BIN = "restoreOptions";

    @BeforeAll
    static void setUp() {
        KEY1 = new Key(SOURCE_NAMESPACE, SET1, "IT1");
    }

    @BeforeEach
    public void setUpEach() {
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
    }

    @Test
    void recordsPerSecond() {
        ASBench.on(SOURCE_NAMESPACE, SET1)
                .keys(10)
                .batchSize(1)
                .threads(1)
                .recordSize(1_000_000)
                .run();
        numberOfRecordsToBackup = AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE);
        assertThat(numberOfRecordsToBackup).isEqualTo(10);
        BackupResult backupResult = CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR).run();
        long startTime = System.currentTimeMillis();
        CliRestore.on(SOURCE_NAMESPACE, backupResult.getBackupDir()).setRecordsPerSecond(1).run();
        long duration = System.currentTimeMillis() - startTime;
        AerospikeLogger.info("Restore for " + numberOfRecordsToBackup + " records took " + duration + " milliseconds");
        assertThat(duration / 1000).isGreaterThan(8).isLessThan(12);
    }

    @Test
    @Disabled // The restore time for a single record is always less the one millisecond. The test fails.
    void totalTimeout() {
        final int ITERATIONS = 1_000_000;
        createBigRecord(ITERATIONS, KEY1);
        BackupResult backupResult = CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR).run();
        assertThatThrownBy(() -> {
            CliRestore.on(SOURCE_NAMESPACE, backupResult.getBackupDir()).setTotalTimeout(1).run();
        }).isInstanceOf(BackupProcessException.class)
                .hasMessageContaining("command execution timed out on client");
    }

    @Test
    @Disabled // The restore time for a single record is always less the one millisecond. The test fails.
    void socketTimeout() {
        final int ITERATIONS = 1_000_000;
        createBigRecord(ITERATIONS, KEY1);
        BackupResult backupResult = CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR).run();
        assertThatThrownBy(() -> {
            CliRestore.on(SOURCE_NAMESPACE, backupResult.getBackupDir()).setSocketTimeout(1).run();
        }).isInstanceOf(BackupProcessException.class)
                .hasMessageContaining("ResultCode: TIMEOUT");
    }

    @Test
    void ignoreRecordError() {
        String recordValue = "valueOfTheRecordForIgnoreRecordError";
        AerospikeDataUtils.put(KEY1, STRING_BIN, recordValue);

        BackupResult backupResult = CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR, 1).run();

        // Enlarge the backup record to be 6mb
        String backupFilePath = backupResult.getBackupDir() + "/" + AutoUtils.getFileNameFromDir(backupResult.getBackupDir());
        String bigString = "x".repeat(6 * 1024 * 1024);
        CliBackupUtils.replaceRecordValueInBackupFile(backupFilePath, recordValue, bigString);

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        assertThatThrownBy(() -> {
            CliRestore.on(SOURCE_NAMESPACE, backupResult.getBackupDir()).run();
        }).isInstanceOf(RestoreProcessException.class)
                .hasMessageContaining("RECORD_TOO_BIG");

        RestoreResult restoreResult = CliRestore.on(SOURCE_NAMESPACE, backupResult.getBackupDir()).setIgnoreRecordError().run();
        assertThat(restoreResult.getIgnoredRecords()).isEqualTo(1);
    }

    private void createBigRecord(int number, Key key) {
        List<Integer> integers = IntStream.range(0, number).boxed().toList();
        Bin bin = new Bin("list", integers);
        WritePolicy policy = new WritePolicy(srcClient.getWritePolicyDefault());
        policy.setTimeout(5_000);
        srcClient.put(policy, key, bin);
    }
}
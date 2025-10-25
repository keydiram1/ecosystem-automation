package api.cli.end2end.xdr;

import api.cli.BackupResult;
import api.cli.CliBackup;
import api.cli.CliRestore;
import com.aerospike.client.Key;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.ASBench;
import utils.AerospikeLogger;
import utils.AutoUtils;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.abs.AerospikeDataUtils;
import utils.cliBackup.CliBackupRunner;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("XDR-CLI-BACKUP")
class XdrBackupTest extends CliBackupRunner {
    private static final String STRING_BIN = "fastRestoreBin";
    private static final String SET1 = "SetFirstRestore";
    private static Key KEY1;
    private static final String SOURCE_NAMESPACE = "source-ns8";
    private static final String DC = "DcXdrBackupTest";
    private static final String BACKUP_DIR = "XdrBackupTestDir";
    private static final int LOCAL_PORT = 9091;

    @BeforeAll
    static void setUp() {
        KEY1 = new Key(SOURCE_NAMESPACE, SET1, "IT1");
    }

    @BeforeEach
    public void setUpEach() {
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
    }

    @Test
    void rewind() {
        for (int i = 0; i < 6; i++) {
            Key key = new Key(SOURCE_NAMESPACE, SET1, "IT1" + i);
            String binValue = "value" + i;
            AerospikeDataUtils.put(key, STRING_BIN, binValue);
            AutoUtils.sleep(1000);
        }
        AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE);

        BackupResult backupResult = CliBackup.onWithXdr(SOURCE_NAMESPACE, BACKUP_DIR)
                .setLocalAddress()
                .setDc(DC)
                .setRewind(4)
                .setLocalPort(LOCAL_PORT)
                .run();

        assertThat(backupResult.getFilesWritten()).isGreaterThan(1).isLessThan(5);
    }

    @Test
    void parallelWrites() {
        String firstValueCreate = "firstValueCreate" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, STRING_BIN, firstValueCreate);

        ASBench.on(SOURCE_NAMESPACE, SET1).duration(1).run();

        BackupResult backupResult = CliBackup.onWithXdr(SOURCE_NAMESPACE, BACKUP_DIR)
                .setLocalAddress()
                .setDc(DC)
                .setLocalPort(LOCAL_PORT)
                .setParallelWrite(9)
                .run();

        assertThat(backupResult.getFilesWritten()).isEqualTo(9);

        backupResult = CliBackup.onWithXdr(SOURCE_NAMESPACE, BACKUP_DIR)
                .setLocalAddress()
                .setDc(DC)
                .setLocalPort(LOCAL_PORT)
                .setParallelWrite(12)
                .run();

        assertThat(backupResult.getFilesWritten()).isEqualTo(12);
    }

    @Test
    void fileLimit() {
        int fileLimit = 1;

        String firstValueCreate = "firstValueCreate" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, STRING_BIN, firstValueCreate);

        BackupResult backupResult = CliBackup.onWithXdr(SOURCE_NAMESPACE, BACKUP_DIR)
                .setLocalAddress()
                .setDc(DC)
                .setLocalPort(LOCAL_PORT)
                .setFileLimit(fileLimit)
                .run();
        assertThat(backupResult.getFilesWritten()).isGreaterThan(9).isLessThan(19);

        backupResult = CliBackup
                .onWithXdr(SOURCE_NAMESPACE, BACKUP_DIR)
                .setLocalAddress()
                .setDc(DC)
                .setLocalPort(LOCAL_PORT)
                .run();
        assertThat(backupResult.getFilesWritten()).isLessThan(9);
    }

    @Test
    void infoPolingPeriod() {
        ASBench.on(SOURCE_NAMESPACE, SET1).duration(1).run();
        int numberOfRecordsToBackup = AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE);

        BackupResult backupInfoPollingEvery5Seconds = CliBackup.onWithXdr(SOURCE_NAMESPACE, BACKUP_DIR)
                .setLocalAddress()
                .setDc(DC)
                .setLocalPort(LOCAL_PORT)
                .setInfoPollingPeriod(5_000)
                .setVerbose()
                .run();

        assertThat(backupInfoPollingEvery5Seconds.getRecordsRead()).isEqualTo(numberOfRecordsToBackup);

        BackupResult backupInfoPollingEvery10Milliseconds = CliBackup.onWithXdr(SOURCE_NAMESPACE, BACKUP_DIR)
                .setLocalAddress()
                .setDc(DC)
                .setLocalPort(LOCAL_PORT)
                .setInfoPollingPeriod(10)
                .setVerbose()
                .run();

        assertThat(backupInfoPollingEvery10Milliseconds.getRecordsRead()).isEqualTo(numberOfRecordsToBackup);

        int numberOfInfoRequestsWhenFlagIs5Seconds = StringUtils.countMatches(backupInfoPollingEvery5Seconds.getFullLog(), "Recoveries");
        int numberOfInfoRequestsWhenFlagIs10Milliseconds = StringUtils.countMatches(backupInfoPollingEvery10Milliseconds.getFullLog(), "Recoveries");
        AerospikeLogger.info("backupInfoPollingEvery5Seconds=" + numberOfInfoRequestsWhenFlagIs5Seconds);
        AerospikeLogger.info("backupInfoPollingEvery1Millisecond=" + numberOfInfoRequestsWhenFlagIs10Milliseconds);
        assertThat(numberOfInfoRequestsWhenFlagIs5Seconds)
                .isGreaterThan(1)
                .isLessThan(20);
        assertThat(numberOfInfoRequestsWhenFlagIs10Milliseconds)
                .isGreaterThan(20)
                .isLessThan(1000);

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE)).isEqualTo(0);

        CliRestore.on(SOURCE_NAMESPACE, backupInfoPollingEvery10Milliseconds.getBackupDir()).run();

        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE)).isEqualTo(numberOfRecordsToBackup);
    }

    @Test
    void backupEmptyNS() {
        BackupResult emptyBackup = CliBackup.onWithXdr(SOURCE_NAMESPACE, "emptyBackup", DC, LOCAL_PORT).run();

        assertThat(emptyBackup.getRecordsRead()).isEqualTo(0);
        assertThat(emptyBackup.getFilesWritten()).isEqualTo(0);
        assertThat(emptyBackup.getBytesWritten()).isEqualTo(0);
        assertThat(emptyBackup.getUdfsRead()).isEqualTo(0);
        assertThat(emptyBackup.getSIndexRead()).isEqualTo(0);

        assertThat(AutoUtils.countFilesInDir(emptyBackup.getBackupDir())).isEqualTo(0);
    }
}
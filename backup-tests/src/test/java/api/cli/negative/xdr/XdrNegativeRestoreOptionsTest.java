package api.cli.negative.xdr;

import api.cli.BackupResult;
import api.cli.CliBackup;
import api.cli.CliRestore;
import api.cli.RestoreProcessException;
import com.aerospike.client.Key;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.AutoUtils;
import utils.aerospike.abs.AerospikeDataUtils;
import utils.cliBackup.CliBackupRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("CLI-BACKUP-NEGATIVE")
@Disabled
class XdrNegativeRestoreOptionsTest extends CliBackupRunner {
    private static final String SOURCE_NAMESPACE = "source-ns3";
    private static Key KEY1;
    private static final String SET1 = "SetFastRestore";
    private static final String STRING_BIN = "fastRestoreBin";
    private static BackupResult negativeRestoreBackup;
    private static final String DC = "DcNegativeRestore";
    private static final int LOCAL_PORT = 8090;

    @BeforeAll
    static void setUp() {
        KEY1 = new Key(SOURCE_NAMESPACE, SET1, "IT1");
        String firstValueCreate = "firstValueCreate" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, STRING_BIN, firstValueCreate);

        negativeRestoreBackup =
                CliBackup.onWithXdr(SOURCE_NAMESPACE, "XdrNegativeDir", DC, LOCAL_PORT)
                        .run();
    }

    @Test
    void unsupportedFlagFilterBin() {
        assertThatThrownBy(() -> {
            CliRestore.on(SOURCE_NAMESPACE, negativeRestoreBackup.getBackupDir())
                    .setBinList("binlist")
                    .run();
        }).isInstanceOf(RestoreProcessException.class)
                .hasMessageContaining("bin list is not supported for ASBX");
    }

    @Test
    void unsupportedFlagNoRecords() {
        assertThatThrownBy(() -> {
            CliRestore.on(SOURCE_NAMESPACE, negativeRestoreBackup.getBackupDir())
                    .setNoRecords()
                    .run();
        }).isInstanceOf(RestoreProcessException.class)
                .hasMessageContaining("no records is not supported for ASBX");
    }

    @Test
    void unsupportedFlagNoUdf() {
        assertThatThrownBy(() -> {
            CliRestore.on(SOURCE_NAMESPACE, negativeRestoreBackup.getBackupDir())
                    .setNoUdf()
                    .run();
        }).isInstanceOf(RestoreProcessException.class)
                .hasMessageContaining("no udfs is not supported for ASBX");
    }

    @Test
    void unsupportedFlagNoSecondaryIndexes() {
        assertThatThrownBy(() -> {
            CliRestore.on(SOURCE_NAMESPACE, negativeRestoreBackup.getBackupDir())
                    .setNoSecondaryIndexes()
                    .run();
        }).isInstanceOf(RestoreProcessException.class)
                .hasMessageContaining("no indexes is not supported for ASBX");
    }

    @Test
    void unsupportedFlagExtraTtl() {
        assertThatThrownBy(() -> {
            CliRestore.on(SOURCE_NAMESPACE, negativeRestoreBackup.getBackupDir())
                    .setExtraTtl(3600)
                    .run();
        }).isInstanceOf(RestoreProcessException.class)
                .hasMessageContaining("extra ttl value is not supported for ASBX");
    }

    @Test
    void unsupportedFlagSetList() {
        assertThatThrownBy(() -> {
            CliRestore.on(SOURCE_NAMESPACE, negativeRestoreBackup.getBackupDir())
                    .setSetList("parentDir")
                    .run();
        }).isInstanceOf(RestoreProcessException.class)
                .hasMessageContaining("set list is not supported for ASBX");
    }

    @Test
    void corruptedBackupFile() {
        BackupResult corruptedFileBackup =
                CliBackup.onWithXdr(SOURCE_NAMESPACE, "corruptedFileBackup", DC, LOCAL_PORT)
                        .run();

        AutoUtils.replaceFileContent(
                corruptedFileBackup.getBackupDir() + "/" + AutoUtils.getFileNameFromDir(corruptedFileBackup.getBackupDir()),
                "corrupted backup file"
        );

        assertThatThrownBy(() -> {
            CliRestore.on(SOURCE_NAMESPACE, corruptedFileBackup.getBackupDir())
                    .run();
        }).isInstanceOf(RestoreProcessException.class)
                .hasMessageContaining("Error: failed to asbx restore: error reading asbx header: unexpected EOF");
    }

    @Test
    void wrongBackupFileExtension() {
        BackupResult corruptedFileBackup =
                CliBackup.onWithXdr(SOURCE_NAMESPACE, "wrongBackupFileExtension", DC, LOCAL_PORT)
                        .run();

        String wrongFileExtension = "file.txt";
        String backupFilePath = corruptedFileBackup.getBackupDir() + "/" + AutoUtils.getFileNameFromDir(corruptedFileBackup.getBackupDir());
        AutoUtils.renameFile(backupFilePath, wrongFileExtension);

        assertThat(AutoUtils.countFilesInDir(corruptedFileBackup.getBackupDir())).isGreaterThan(0);

        assertThatThrownBy(() -> {
            CliRestore.on(SOURCE_NAMESPACE, corruptedFileBackup.getBackupDir())
                    .run();
        }).isInstanceOf(RestoreProcessException.class)
                .hasMessageContaining("Error: failed to create backup reader: empty storage: /tmp/wrongBackupFileExtension is empty");
    }
}
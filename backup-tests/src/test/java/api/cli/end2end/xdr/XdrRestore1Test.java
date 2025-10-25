package api.cli.end2end.xdr;

import api.cli.BackupProcessException;
import api.cli.BackupResult;
import api.cli.CliBackup;
import api.cli.CliRestore;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import org.junit.jupiter.api.*;
import utils.AutoUtils;
import utils.aerospike.abs.AerospikeDataUtils;
import utils.cliBackup.CliBackupRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("XDR-CLI-BACKUP")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class XdrRestore1Test extends CliBackupRunner {
    private static final String STRING_BIN = "RestoreTestBin";
    private static final String SET1 = "SetFullBackupTest1";
    private static final String SET2 = "SetFullBackupTest2";
    private static final String SET3 = "SetFullBackupTest3";
    private static Key KEY1;
    private static Key KEY2;
    private static Key KEY3;
    private static final String SOURCE_NAMESPACE = "source-ns2";
    private static final String DC = "DcXdrRestoreTest";
    private static final String BACKUP_DIR = "XdrRestoreTestDir";
    private static final int LOCAL_PORT = 8087;

    @BeforeAll
    static void setUp() {
        KEY1 = new Key(SOURCE_NAMESPACE, SET1, "IT1");
        KEY2 = new Key(SOURCE_NAMESPACE, SET2, "IT2");
        KEY3 = new Key(SOURCE_NAMESPACE, SET3, "IT3");
    }

    @BeforeEach
    public void setUpEach() {
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
    }

    @Test
    @Order(1)
    void restoreBackup() {
        String firstValueCreate = "firstValueCreate" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, STRING_BIN, firstValueCreate);
        AutoUtils.sleep(3000);
        long startTime = System.currentTimeMillis();
        BackupResult createFirstValue = CliBackup.onWithXdr(SOURCE_NAMESPACE, "createFirstValue", DC, LOCAL_PORT)
                .run();
        assertThat(createFirstValue.getRecordsRead()).isEqualTo(1);
        assertThat(createFirstValue.getFilesWritten()).isEqualTo(1);
        assertThat(createFirstValue.getBytesWritten()).isGreaterThan(100).isLessThan(3500);
        assertThat(createFirstValue.getExitCode()).isEqualTo(0);
        assertThat(createFirstValue.getRecordsRead()).isEqualTo(1);
        assertThat(createFirstValue.getDurationMillis()).isGreaterThan(1).isLessThan(1000);
        if (AutoUtils.isRunningOnMacos()) {
            assertThat(createFirstValue.getStartTime())
                    .isBetween(startTime - 10_000, startTime + 10_000);
        } else {
            assertThat(createFirstValue.getStartTime())
                    .isBetween(startTime - 10_800_000 - 10_000, startTime - 10_800_000 + 10_000);
        }

        BackupResult keyNoChanges = CliBackup.onWithXdr(SOURCE_NAMESPACE, "noChanges", DC, LOCAL_PORT)
                .run();
        assertThat(keyNoChanges.getBackupDir()).isNotEqualTo(createFirstValue.getBackupDir());

        String firstValueUpdate = "firstValueUpdate" + System.currentTimeMillis();
        String secondValueCreate = "secondValueCreate" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, STRING_BIN, firstValueUpdate);
        AerospikeDataUtils.put(KEY2, STRING_BIN, secondValueCreate);
        BackupResult firstValueUpdateSecondValueCreate =
                CliBackup.onWithXdr(SOURCE_NAMESPACE, "firstValueUpdateSecondValueCreate", DC, LOCAL_PORT)
                        .run();
        assertThat(firstValueUpdateSecondValueCreate.getBytesWritten()).isGreaterThan(3000).isLessThan(6000);

        String secondValueUpdate = "secondValueUpdate" + System.currentTimeMillis();
        String thirdValueCreate = "thirdValueCreate" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY2, STRING_BIN, secondValueUpdate);
        AerospikeDataUtils.put(KEY3, STRING_BIN, thirdValueCreate);
        AerospikeDataUtils.delete(KEY1);
        BackupResult keyFirstValueDeleteSecondValueUpdateThirdValueCreate =
                CliBackup.onWithXdr(SOURCE_NAMESPACE, "keyFirstValueDeleteSecondValueUpdateThirdValueCreate", DC, LOCAL_PORT)
                        .run();

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        CliRestore.on(SOURCE_NAMESPACE, keyNoChanges.getBackupDir()).run();
        Record retrievedRecord = srcClient.get(null, KEY1);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(firstValueCreate);

        CliRestore.on(SOURCE_NAMESPACE, createFirstValue.getBackupDir()).run();
        retrievedRecord = srcClient.get(null, KEY1);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(firstValueCreate);

        CliRestore.on(SOURCE_NAMESPACE, firstValueUpdateSecondValueCreate.getBackupDir()).run();
        retrievedRecord = srcClient.get(null, KEY1);
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(firstValueUpdate);
        retrievedRecord = srcClient.get(null, KEY2);
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(secondValueCreate);
        retrievedRecord = srcClient.get(null, KEY3);
        assertThat(retrievedRecord).isNull();

        CliRestore.on(SOURCE_NAMESPACE, keyFirstValueDeleteSecondValueUpdateThirdValueCreate.getBackupDir()).run();
        retrievedRecord = srcClient.get(null, KEY1);
        // For now, we don't support delete actions in incremental backup so the first record will stay as it was.
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(firstValueUpdate);
        retrievedRecord = srcClient.get(null, KEY2);
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(secondValueUpdate);
        retrievedRecord = srcClient.get(null, KEY3);
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(thirdValueCreate);
    }

    @Test
    void removeFiles() {
        String firstValueCreate = "firstValueCreate" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, STRING_BIN, firstValueCreate);

        CliBackup.onWithXdr(SOURCE_NAMESPACE, BACKUP_DIR)
                .setLocalAddress()
                .setDc(DC)
                .setLocalPort(LOCAL_PORT)
                .run();
        assertThatThrownBy(() -> {
            CliBackup.onWithXdr(SOURCE_NAMESPACE, BACKUP_DIR)
                    .setLocalAddress()
                    .setDc(DC)
                    .setLocalPort(LOCAL_PORT)
                    .run(false, true);
        }).isInstanceOf(BackupProcessException.class)
                .hasMessageContaining("backup folder must be empty");

        BackupResult backupResult = CliBackup.onWithXdr(SOURCE_NAMESPACE, BACKUP_DIR)
                .setLocalAddress()
                .setDc(DC)
                .setLocalPort(LOCAL_PORT)
                .setRemoveFiles()
                .run(false, true);

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        CliRestore.on(SOURCE_NAMESPACE, backupResult.getBackupDir()).run();

        Record retrievedRecord = srcClient.get(null, KEY1);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(firstValueCreate);
    }

    @Test
    void noGenerationXdr() {
        String updateDir = "updateDir";
        String firstValueCreate = "firstValueCreate" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, STRING_BIN, firstValueCreate);
        AutoUtils.sleep(3000);
        BackupResult backupCreate = CliBackup.onWithXdr(SOURCE_NAMESPACE, BACKUP_DIR, DC, LOCAL_PORT).run();
        Record retrievedRecord = srcClient.get(null, KEY1);
        assertThat(retrievedRecord.generation).isEqualTo(1);

        String firstValueUpdate = "firstValueUpdate" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, STRING_BIN, firstValueUpdate);
        AutoUtils.sleep(3000);
        BackupResult backupUpdate = CliBackup.onWithXdr(SOURCE_NAMESPACE, updateDir, DC, LOCAL_PORT).run();

        retrievedRecord = srcClient.get(null, KEY1);
        assertThat(retrievedRecord.generation).isEqualTo(2);

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        retrievedRecord = srcClient.get(null, KEY1);
        assertThat(retrievedRecord).isNull();

        CliRestore.on(SOURCE_NAMESPACE, backupCreate.getBackupDir()).run();

        retrievedRecord = srcClient.get(null, KEY1);
        assertThat(retrievedRecord.generation).isEqualTo(1);

        CliRestore.on(SOURCE_NAMESPACE, backupUpdate.getBackupDir()).run();
        retrievedRecord = srcClient.get(null, KEY1);
        assertThat(retrievedRecord.generation).isEqualTo(2);

        CliRestore.on(SOURCE_NAMESPACE, backupUpdate.getBackupDir()).setNoGeneration().run();
        retrievedRecord = srcClient.get(null, KEY1);
        assertThat(retrievedRecord.generation).isEqualTo(3);
    }

    @Test
    void xdrBackupRestoreUpdatedWithScanBackupRestore() {
        String firstValueXdrCreate = "firstValueCreate" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, STRING_BIN, firstValueXdrCreate);
        BackupResult firstValueXdrBackup =
                CliBackup.onWithXdr(SOURCE_NAMESPACE, "firstValueXdrBackup", DC, LOCAL_PORT)
                        .run();
        assertThat(firstValueXdrBackup.getRecordsRead()).isEqualTo(1);

        String firstValueScanUpdate = "firstValueUpdate" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, STRING_BIN, firstValueScanUpdate);
        BackupResult updateValueScanBackup =
                CliBackup.on(SOURCE_NAMESPACE, "updateValueScanBackup").run();

        assertThat(updateValueScanBackup.getRecordsRead()).isEqualTo(1);


        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        CliRestore.on(SOURCE_NAMESPACE, firstValueXdrBackup.getBackupDir()).run();
        Record retrievedRecord = srcClient.get(null, KEY1);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(firstValueXdrCreate);

        CliRestore.on(SOURCE_NAMESPACE, updateValueScanBackup.getBackupDir()).run();
        retrievedRecord = srcClient.get(null, KEY1);
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(firstValueScanUpdate);
    }

    @Test
    void restoreEmptyString() {
        String emptyValue = "";
        AerospikeDataUtils.put(KEY1, STRING_BIN, emptyValue);

        BackupResult backupResult = CliBackup.onWithXdr(SOURCE_NAMESPACE, "emptyStringBackup", DC, LOCAL_PORT).run();

        assertThat(backupResult.getRecordsRead()).isEqualTo(1);

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        CliRestore.on(SOURCE_NAMESPACE, backupResult.getBackupDir()).run();

        Record retrievedRecord = srcClient.get(null, KEY1);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(emptyValue);
    }

    @Test
    void xdrRestoreEmptySetName() {
        Key keyWithoutSet = new Key(SOURCE_NAMESPACE, "", "NoSetKey");
        String value = "NoSetValue" + System.currentTimeMillis();

        AerospikeDataUtils.put(keyWithoutSet, STRING_BIN, value);

        BackupResult backupResult = CliBackup.onWithXdr(SOURCE_NAMESPACE, "noSetBackup", DC, LOCAL_PORT).run();

        assertThat(backupResult.getRecordsRead()).isEqualTo(1);

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        CliRestore.on(SOURCE_NAMESPACE, backupResult.getBackupDir()).run();

        Record retrievedRecord = srcClient.get(null, keyWithoutSet);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(value);
    }

    @Test
    void backupRestoreVeryLongKey() {
        String longKey = "L".repeat(3000); // 3000 characters long
        Key longKeyRecord = new Key(SOURCE_NAMESPACE, SET1, longKey);

        String value = "LongKeyValue" + System.currentTimeMillis();
        AerospikeDataUtils.put(longKeyRecord, STRING_BIN, value);

        BackupResult backupResult = CliBackup.onWithXdr(SOURCE_NAMESPACE, "longKeyBackup", DC, LOCAL_PORT).run();

        assertThat(backupResult.getRecordsRead()).isEqualTo(1);

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        CliRestore.on(SOURCE_NAMESPACE, backupResult.getBackupDir()).run();

        Record retrievedRecord = srcClient.get(null, longKeyRecord);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(value);
    }

    @Test
    void backupRestoreSpecialCharacterKeyAndValue() {
        String specialKey = "Spécial Key! @#$%^&*()_+= 世界@#$%^&*()_+=";
        String specialValue = "Välüé with 🏆 emojis & 特殊字符@#$%^&*()_+=";

        Key specialCharKey = new Key(SOURCE_NAMESPACE, SET1, specialKey);

        AerospikeDataUtils.put(specialCharKey, STRING_BIN, specialValue);

        BackupResult backupResult = CliBackup.onWithXdr(SOURCE_NAMESPACE, "specialCharKeyValueBackup", DC, LOCAL_PORT).run();

        assertThat(backupResult.getRecordsRead()).isEqualTo(1);

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        CliRestore.on(SOURCE_NAMESPACE, backupResult.getBackupDir()).run();

        Record retrievedRecord = srcClient.get(null, specialCharKey);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(specialValue);
    }
}
package api.cli.tlsEnv.scan;

import api.cli.BackupResult;
import api.cli.CliBackup;
import api.cli.CliRestore;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.abs.AerospikeDataUtils;
import utils.cliBackup.CliBackupRunner;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("BACKUP-TLS-ENV")
class ScanTlsEdgeCasesTest extends CliBackupRunner {
    private static final String STRING_BIN = "RestoreTestBin";
    private static final String SET_NAME = "SetFullBackupTest";
    private static Key KEY1;
    private static final String SOURCE_NAMESPACE = "source-ns1";

    @BeforeAll
    static void setUp() {
        KEY1 = new Key(SOURCE_NAMESPACE, SET_NAME, "IT1");
    }


    @BeforeEach
    public void setUpEach() {
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
    }

    @Test
    void lotsOfFlags() {
        String firstValueCreate = "firstValueCreate" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, STRING_BIN, firstValueCreate);

        BackupResult backupResult = CliBackup.onWithTls(SOURCE_NAMESPACE, "lotsOfFlagsTestBackup").setSecretAgent()
                .setEncryptionKeySecret("secrets:encKey:encryption-key-pem").setEncryptionMode("aes128").setCompressMode("ZSTD").setCompressLevel(5)
                .setBandwidth(1000).setSocketTimeout(10000).setCompact().setTotalTimeout(10000).setBinList(STRING_BIN).setFileLimit(1000)
                .setRecordsPerSecond(1000).setSetList(SET_NAME).setOutputFile("newFileName")
                .runWithTls();

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        CliRestore.onWithTls(SOURCE_NAMESPACE, backupResult.getBackupDir()).setSecretAgent()
                .setEncryptionKeySecret("secrets:encKey:encryption-key-pem").setEncryptionMode("aes128").setCompressMode("ZSTD")
                .setSocketTimeout(10000).setTotalTimeout(10000).setBandwidth(10000).setRecordsPerSecond(1000).setBatchSize(10000).setMaxAsyncBatches(10000)
                .setBinList(STRING_BIN).setSetList(SET_NAME).setExtraTtl(1000).setNoSecondaryIndexes().setNoUdf()
                .run();

        Record retrievedRecord = srcClient.get(null, KEY1);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(firstValueCreate);
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET_NAME, SOURCE_NAMESPACE)).isEqualTo(1);
    }
}
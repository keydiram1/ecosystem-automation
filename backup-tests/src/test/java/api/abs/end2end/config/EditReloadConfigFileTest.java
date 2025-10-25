package api.abs.end2end.config;

import api.abs.AbsBackupApi;
import api.abs.AbsConfigApi;
import api.abs.AbsRestoreApi;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.abs.AbsConfigFileUtils;
import utils.abs.AbsRunner;
import utils.aerospike.abs.AerospikeDataUtils;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("ABS-SEQUENTIAL-TESTS-2")
class EditReloadConfigFileTest extends AbsRunner {
    private static final String STRING_BIN = "reloadBin";
    private static final String SET1 = "SetReload";
    private static Key KEY1;
    private static String SOURCE_NAMESPACE = "source-ns1";

    @BeforeEach
    public void setUpEach() {
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
    }

    @Test
    @Disabled
    void editReloadConfigFile() {
        String routineName = "generatedRoutine";
        AbsConfigFileUtils.addRoutine(routineName, "source-ns1");
        AbsConfigApi.apply();

        KEY1 = new Key(SOURCE_NAMESPACE, SET1, "IT1");
        String firstValueCreate = "firstValueCreate" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, STRING_BIN, firstValueCreate);

        String backupKey = AbsBackupApi.startFullBackupSync(routineName).getKey();

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        AbsRestoreApi.restoreFullSync(backupKey, routineName);

        Record retrievedRecord = AerospikeDataUtils.get(KEY1);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(firstValueCreate);
    }
}
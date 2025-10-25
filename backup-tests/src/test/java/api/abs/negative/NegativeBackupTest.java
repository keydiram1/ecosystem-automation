package api.abs.negative;

import api.abs.AbsBackupApi;
import api.abs.AbsRestoreApi;
import api.abs.AbsRoutineApi;
import api.abs.generated.ApiException;
import api.abs.generated.model.DtoBackupDetails;
import com.aerospike.client.query.IndexType;
import org.apache.http.HttpStatus;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import utils.ASBench;
import utils.abs.AbsRunner;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.abs.AerospikeDataUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisabledIfSystemProperty(named = "qa_environment", matches = "GCP")
@Tag("ABS-NEGATIVE-TESTS")
public class NegativeBackupTest extends AbsRunner {

    @Test
    void dontBackupSecondaryIndex() {
        String routineName = "noIndexesUdfsRecords";
        String sourceNs = AbsRoutineApi.getAnyNamespaceForRoutine(routineName);

        String indexName = "dontBackupSecondaryIndex";
        if (!AerospikeDataUtils.isIndexExist(indexName))
            srcClient.createIndex(null, sourceNs, "set", indexName, "bin", IndexType.NUMERIC);
        assertThat(AerospikeDataUtils.isIndexExist(indexName)).isTrue();

        DtoBackupDetails backup = AbsBackupApi.startFullBackupSync(routineName);
        assertThat(backup.getSecondaryIndexCount()).isZero();
    }

    @Test
    void dontBackupUdf() {
        String routineName = "noIndexesUdfsRecords";

        String fileName = "myFullUdf.lua";
        AerospikeDataUtils.createUDF(fileName);
        assertThat(AerospikeDataUtils.isUdfExist(fileName)).isTrue();

        DtoBackupDetails backup = AbsBackupApi.startFullBackupSync(routineName);
        assertThat(backup.getUdfCount()).isZero();
    }

    @Test
    void dontBackupRecords() {
        String routineName = "noIndexesUdfsRecords";
        String sourceNs = AbsRoutineApi.getAnyNamespaceForRoutine(routineName);
        String setName = "setNoBackup";

        ASBench.on(sourceNs, setName).duration(1).run();
        int numberOfRecordsBeforeTruncate = AerospikeCountUtils.getSetObjectCount(srcClient, setName, sourceNs);
        assertThat(numberOfRecordsBeforeTruncate).isGreaterThan(100);

        DtoBackupDetails backup = AbsBackupApi.startFullBackupSync(routineName);
        assertThat(backup.getRecordCount()).isZero();
    }

    @Test
    void dontBackupConfFile() {
        String routineName = "fullBackup1";
        String sourceNs = AbsRoutineApi.getAnyNamespaceForRoutine(routineName);
        String setName = "noConf";

        ASBench.on(sourceNs, setName).duration(1).run();
        int numberOfRecordsBeforeTruncate = AerospikeCountUtils.getSetObjectCount(srcClient, setName, sourceNs);
        assertThat(numberOfRecordsBeforeTruncate).isGreaterThan(100);

        DtoBackupDetails backup = AbsBackupApi.startFullBackupSync(routineName);
        long created = AbsBackupApi.getCreated(backup);

        Assertions.assertThatThrownBy(() -> AbsRestoreApi.retrieveAsConfFile(routineName, created))
                .hasMessageContaining("no configuration backups found for " + routineName)
                .extracting(it -> ((ApiException) it).getCode(), InstanceOfAssertFactories.INTEGER)
                .isEqualTo(HttpStatus.SC_NOT_FOUND);
    }
}

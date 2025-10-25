package api.abs.load.aws.restore;

import api.abs.AbsBackupApi;
import api.abs.AbsRestoreApi;
import api.abs.AbsRoutineApi;
import api.abs.generated.model.DtoBackupDetails;
import api.abs.generated.model.DtoRestoreJobStatus;
import api.abs.generated.model.DtoRestorePolicy;
import api.abs.load.aws.LoadRunner;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import utils.AerospikeLogger;
import utils.ConfigParametersHandler;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.abs.AerospikeDataUtils;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


class LoadRestoreParent extends LoadRunner {

    private int numberOfRecordsBeforeTruncate;
    private DtoBackupDetails backup;
    private String sourceNamespace;
    protected String routineName;
    protected Key KEY2;
    protected long dataSizeBeforeTruncate;

    protected void setUpParent() {
        minutesToWaitForAllLoadClassesToFinishSetup = LOAD_LEVEL.equals("low") ? 5 : 60;
        sourceNamespace = AbsRoutineApi.getAnyNamespaceForRoutine(routineName);
        KEY2 = new Key(sourceNamespace, SET2, "IT1");
        AerospikeDataUtils.truncateSourceNamespace(sourceNamespace);

        createBigData(sourceNamespace);
        createData();

        numberOfRecordsBeforeTruncate = AerospikeCountUtils.getSetObjectCount(srcClient, SET, sourceNamespace);
        dataSizeBeforeTruncate = AerospikeDataUtils.getDataTotalBytes(sourceNamespace);
        AerospikeLogger.info("Data size before truncate: " + dataSizeBeforeTruncate);

        try {
            backup = AbsBackupApi.startFullBackupSync(routineName);
            AerospikeDataUtils.truncateSourceNamespace(sourceNamespace);
        } finally {
            waitGroup.wait(minutesToWaitForAllLoadClassesToFinishSetup);
        }
    }

    protected void restoreBackupParent() {
        AerospikeLogger.info("The number of records before truncate: " + numberOfRecordsBeforeTruncate);
        var timeout = Duration.ofMinutes(ConfigParametersHandler.getParameter("LOAD_LEVEL").equals("low") ? 1 : 10);

        long restoreStartTime = System.currentTimeMillis();
        DtoRestorePolicy restorePolicy = new DtoRestorePolicy()
                .noGeneration(true)
                .batchSize(1024)
                .maxAsyncBatches(256)
                .parallel(8);
        DtoRestoreJobStatus restoreStatus = AbsRestoreApi.restoreFullSync(backup.getKey(), routineName, restorePolicy, timeout);
        long restoreDuration = System.currentTimeMillis() - restoreStartTime;

        assertThat(restoreDuration).isLessThan(300_000);

        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET, sourceNamespace)).isEqualTo(numberOfRecordsBeforeTruncate);

        assertThat(restoreStatus.getReadRecords()).isEqualTo(numberOfRecordsBeforeTruncate + 1); // plus one due to the record we insert to KEY2

        Record record = AerospikeDataUtils.get(KEY2);

        assertThat(record).isNotNull();
        assertThat(record.getString("stringBin")).isEqualTo("TEST");
        assertThat(record.getInt("intBin")).isEqualTo(Integer.MAX_VALUE);
        assertThat(record.getValue("blobBin")).isEqualTo("blob".getBytes());
        assertThat(record.getList("listBin")).isEqualTo(List.of("a", "b", "c"));
        assertThat(record.getMap("mapBin")).isEqualTo(Map.of("a", true, "b", false));

        long dataSizeAfterRestore = AerospikeDataUtils.getDataTotalBytes(sourceNamespace);
        AerospikeLogger.info("Data size after restore: " + dataSizeAfterRestore);

        assertThat(dataSizeAfterRestore)
                .as("Data size after restore should be equal to the size before taking a backup")
                .isEqualTo(dataSizeBeforeTruncate);
    }

    private void createData() {
        srcClient.put(null, KEY2, new Bin("stringBin", "TEST"));
        srcClient.put(null, KEY2, new Bin("intBin", Integer.MAX_VALUE));
        srcClient.put(null, KEY2, new Bin("blobBin", "blob".getBytes()));
        srcClient.put(null, KEY2, new Bin("listBin", List.of("a", "b", "c")));
        srcClient.put(null, KEY2, new Bin("mapBin", Map.of("a", true, "b", false)));
    }
}

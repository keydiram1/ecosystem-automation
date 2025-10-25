package api.cli.end2end.scan;

import api.cli.BackupResult;
import api.cli.CliBackup;
import api.cli.CliRestore;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.cluster.Partition;
import com.aerospike.client.exp.Exp;
import com.aerospike.client.exp.Expression;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.ASBench;
import utils.AerospikeLogger;
import utils.AutoUtils;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.AerospikeScanner;
import utils.aerospike.abs.AerospikeDataUtils;
import utils.cliBackup.CliBackupRunner;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("CLI-BACKUP")
class BackupOptionsTest2 extends CliBackupRunner {
    private static final String SOURCE_NAMESPACE = "source-ns8";
    private static final String SET1 = "setBackupOptions";
    private static Key KEY1;
    private static final String STRING_BIN = "testBin";
    private static final String BACKUP_DIR = "BackupOptions2Dir";

    @BeforeAll
    static void setUp() {
        KEY1 = new Key(SOURCE_NAMESPACE, SET1, "IT1");
    }

    @BeforeEach
    public void setUpEach() {
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
    }

    @Test
    void afterDigest() {
        ASBench.on(SOURCE_NAMESPACE, SET1).keys(100000).run();
        AerospikeScanner aerospikeScanner = new AerospikeScanner();
        aerospikeScanner.scan(srcClient, SOURCE_NAMESPACE, SET1);

        // list with all the keys
        List<Key> allKeys = aerospikeScanner.getAllKeys();
        int numberOfKeysBefore = allKeys.size();
        Key firstKey = allKeys.get(0);
        int partitionId = Partition.getPartitionId(firstKey.digest);

        // list with keys of the same partition
        List<Key> samePartitionKeys = new ArrayList<>();
        for (Key key : allKeys) {
            if (Partition.getPartitionId(key.digest) == partitionId) {
                samePartitionKeys.add(key);
            }
        }

        // delete all the keys that are not in the same partition
        for (Key key : allKeys) {
            if (!samePartitionKeys.contains(key)) {
                srcClient.delete(null, key);
            }
        }

        int allKeysAfterDeletion = AerospikeCountUtils.getNamespaceObjectCount(srcClient, SOURCE_NAMESPACE);
        int numberOfKeysSamePartition = samePartitionKeys.size();

        int keepRecordsAfterThisKey = 5;
        int expectedRecordsAfterBackup = keepRecordsAfterThisKey - 1;
        Key digestKey = samePartitionKeys.get(samePartitionKeys.size() - keepRecordsAfterThisKey);

        String digestBase64 = Base64.getEncoder().encodeToString(digestKey.digest);

        BackupResult afterDigestBackupResult = CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR).setAfterDigest(digestBase64).run();
        assertThat(expectedRecordsAfterBackup).isEqualTo(afterDigestBackupResult.getRecordsRead());

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        CliRestore.on(SOURCE_NAMESPACE, afterDigestBackupResult.getBackupDir()).run();

        AerospikeLogger.info("number of keys at start " + numberOfKeysBefore);
        AerospikeLogger.info("numberOfKeysSamePartition " + numberOfKeysSamePartition);
        AerospikeLogger.info("allKeysAfterDeletion: " + allKeysAfterDeletion);
        assertThat(expectedRecordsAfterBackup).isEqualTo(AerospikeCountUtils.getNamespaceObjectCount(srcClient, SOURCE_NAMESPACE));
    }

    @Test
    void bandwidth() {
        ASBench.on(SOURCE_NAMESPACE, SET1).keys(5_000).recordSize(1000).run();
        long startTime = System.currentTimeMillis();
        CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR).setBandwidth(1).run();
        long duration = System.currentTimeMillis() - startTime;
        AerospikeLogger.info("Backup took " + duration + " milliseconds");
        assertThat(duration / 1000).isGreaterThan(3).isLessThan(20);

        startTime = System.currentTimeMillis();
        CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR).run();
        duration = System.currentTimeMillis() - startTime;
        AerospikeLogger.info("Backup took " + duration + " milliseconds");
        assertThat(duration / 1000).isLessThan(3);
    }


    @Test
    void compact() {
        ASBench.on(SOURCE_NAMESPACE, SET1).duration(1).recordSize(1024).run();
        String firstValueCreate = "firstValueCreate" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, STRING_BIN, firstValueCreate);

        BackupResult backupResult = CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR, 1).run();
        long bytesWrittenNoCompact = AutoUtils.getSizeOfDir(backupResult.getBackupDir());

        BackupResult backupResultCompact = CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR, 1).setCompact().run();
        long bytesWrittenCompact = AutoUtils.getSizeOfDir(backupResultCompact.getBackupDir());

        assertThat(bytesWrittenCompact).isLessThan(bytesWrittenNoCompact);
        assertThat(bytesWrittenCompact).isEqualTo(backupResultCompact.getBytesWritten());

        CliRestore.on(SOURCE_NAMESPACE, backupResultCompact.getBackupDir()).run();

        Record retrievedRecord = srcClient.get(null, KEY1);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getString(STRING_BIN)).isEqualTo(firstValueCreate);
    }

    @Test
    void partitionList() {
        ASBench.on(SOURCE_NAMESPACE, SET1).keys(100000).run();
        AerospikeScanner aerospikeScanner = new AerospikeScanner();
        aerospikeScanner.scan(srcClient, SOURCE_NAMESPACE, SET1);

        // List with all the keys
        List<Key> allKeys = aerospikeScanner.getAllKeys();
        int numberOfKeysBefore = allKeys.size();
        Key firstKey = allKeys.get(0);
        int partitionId = Partition.getPartitionId(firstKey.digest);

        List<Key> samePartitionKeys = AerospikeDataUtils.filterKeysByPartition(allKeys, partitionId);

        int numberOfKeysSamePartition = samePartitionKeys.size();
        AerospikeLogger.info("Total number of keys at start: " + numberOfKeysBefore);
        AerospikeLogger.info("Number of keys in the same partition: " + numberOfKeysSamePartition);

        // Use a string filter for the partition range
        String partitionFilter = partitionId + "-1";

        BackupResult partitionBackupResult = CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR)
                .setPartitionList(partitionFilter)
                .run();
        AerospikeLogger.info("Records backed up using partition filter '" + partitionFilter + "': " + partitionBackupResult.getRecordsRead());

        assertThat(partitionBackupResult.getRecordsRead() < numberOfKeysBefore);

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        CliRestore.on(SOURCE_NAMESPACE, partitionBackupResult.getBackupDir()).run();

        // Validate that only the records from the specified partition are restored
        int restoredRecordCount = AerospikeCountUtils.getNamespaceObjectCount(srcClient, SOURCE_NAMESPACE);
        AerospikeLogger.info("Restored record count: " + restoredRecordCount);
        assertThat(numberOfKeysSamePartition).isEqualTo(restoredRecordCount);
        assertThat(numberOfKeysBefore).isGreaterThan(numberOfKeysSamePartition);
    }

    @Test
    void filterExpression() {
        srcClient.put(null, new Key(SOURCE_NAMESPACE, "groupA", "key1"), new Bin("testbin", 30));
        srcClient.put(null, new Key(SOURCE_NAMESPACE, "groupB", "key2"), new Bin("testbin", 60));
        srcClient.put(null, new Key(SOURCE_NAMESPACE, "groupC", "key3"), new Bin("testbin", 80));
        srcClient.put(null, new Key(SOURCE_NAMESPACE, "groupA", "key4"), new Bin("testbin", 100));

        AerospikeScanner aerospikeScanner = new AerospikeScanner();
        aerospikeScanner.scan(srcClient, SOURCE_NAMESPACE, null);
        List<Key> allKeys = aerospikeScanner.getAllKeys();
        assertThat(allKeys.size()).isEqualTo(4);

        // Build a filter expression to select records where set_name is "groupA" or "groupB"
        Expression filterExp = Exp.build(
                Exp.or(
                        Exp.eq(Exp.setName(), Exp.val("groupA")),
                        Exp.eq(Exp.setName(), Exp.val("groupB"))
                )
        );

        // Encode the expression in Base64 for use with asbackup
        String base64EncodedFilterExpression = filterExp.getBase64();
        System.out.println("Base64 Encoded Filter Expression: " + base64EncodedFilterExpression);

        BackupResult filteredBackupResult = CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR)
                .setFilterExpression(base64EncodedFilterExpression)
                .run();

        // Verify that only records with set_name "groupA" or "groupB" were backed up
        int recordsBackedUp = filteredBackupResult.getRecordsRead();
        assertThat(recordsBackedUp).isEqualTo(3); // Expecting 3 records (key1, key2, key4)

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        CliRestore.on(SOURCE_NAMESPACE, filteredBackupResult.getBackupDir()).run();

        int restoredRecordCount = AerospikeCountUtils.getNamespaceObjectCount(srcClient, SOURCE_NAMESPACE);
        assertThat(restoredRecordCount).isEqualTo(3);

        aerospikeScanner.scan(srcClient, SOURCE_NAMESPACE, null);
        List<Key> restoredKeys = aerospikeScanner.getAllKeys();
        for (Key key : restoredKeys) {
            assertThat(key.setName).isIn("groupA", "groupB");
        }
    }
}
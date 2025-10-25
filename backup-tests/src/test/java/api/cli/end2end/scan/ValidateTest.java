package api.cli.end2end.scan;

import api.cli.BackupResult;
import api.cli.CliBackup;
import api.cli.CliRestore;
import com.aerospike.client.Key;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.ASBench;
import utils.AutoUtils;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.abs.AerospikeDataUtils;
import utils.cliBackup.CliBackupRunner;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("CLI-BACKUP")
class ValidateTest extends CliBackupRunner {
    private static final String SOURCE_NAMESPACE = "source-ns17";

    private static final String STRING_BIN = "ValidateBin";
    private static final String SET1 = "SetValidateTest";
    private static Key KEY1;
    private static final String BACKUP_DIR = "ValidateTestDir";

    @BeforeAll
    static void setUp() {
        KEY1 = new Key(SOURCE_NAMESPACE, SET1, "IT1");
    }

    @BeforeEach
    public void setUpEach() {
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
    }

    @Test
    void validateDir1File1Record() {
        String firstValueCreate = "firstValueCreate" + System.currentTimeMillis();
        AerospikeDataUtils.put(KEY1, STRING_BIN, firstValueCreate);

        BackupResult backup = CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR).run();

        String validationString = CliRestore.on().setValidateDirectory(backup.getBackupDir()).validate();

        assertThat(validationString).contains("initializing restore config");
        assertThat(validationString).contains("initializing storage for reader");
        assertThat(validationString).contains("directory=" + backup.getBackupDir());
        assertThat(validationString).contains("initialized local storage reader");
        assertThat(validationString).contains("initializing restore client");
        assertThat(validationString).contains("starting asb validation");
        assertThat(validationString).contains("msg=\"found asb files\" number=1");
        assertThat(validationString).contains("Validation report");
        assertThat(validationString).contains("Start Time:");
        assertThat(validationString).contains("Duration:");
        assertThat(validationString).contains("Records Read:         1");
        assertThat(validationString).contains("sIndex Read:          0");
        assertThat(validationString).contains("UDFs Read:            0");
        assertThat(validationString).contains("Total Bytes Read:     144");
        assertThat(validationString).contains("Exit Code: 0");
    }

    @Test
    void validateDir10Files1000Records() {
        ASBench.on(SOURCE_NAMESPACE, SET1)
                .keys(1_000)
                .batchSize(10)
                .threads(10)
                .recordSize(1)
                .run();

        BackupResult backup = CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR, 10).run();
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE)).isEqualTo(1000);

        String validationString = CliRestore.on().setValidateDirectory(backup.getBackupDir()).validate();

        assertThat(validationString).contains("initializing restore config");
        assertThat(validationString).contains("initializing storage for reader");
        assertThat(validationString).contains("directory=" + backup.getBackupDir());
        assertThat(validationString).contains("initialized local storage reader");
        assertThat(validationString).contains("initializing restore client");
        assertThat(validationString).contains("starting asb validation");
        assertThat(validationString).contains("msg=\"found asb files\" number=10");
        assertThat(validationString).contains("Validation report");
        assertThat(validationString).contains("Start Time:");
        assertThat(validationString).contains("Duration:");
        assertThat(validationString).contains("Records Read:         1000");
        assertThat(validationString).contains("sIndex Read:          0");
        assertThat(validationString).contains("UDFs Read:            0");
        assertThat(validationString).contains("Exit Code: 0");
    }

    @Test
    void validateFile1000Records() {
        ASBench.on(SOURCE_NAMESPACE, SET1)
                .keys(1_000)
                .batchSize(10)
                .threads(10)
                .recordSize(1)
                .run();
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE)).isEqualTo(1000);

        BackupResult backup = CliBackup.on(SOURCE_NAMESPACE, BACKUP_DIR, 1).run();
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE)).isEqualTo(1000);

        String filePath = backup.getBackupDir() + "/" + AutoUtils.getFileNameFromDir(backup.getBackupDir());

        String validationString = CliRestore.on()
                .setValidateFile(filePath)
                .validate();

        assertThat(validationString).contains("initializing restore config");
        assertThat(validationString).contains("initializing storage for reader");
        assertThat(validationString).contains("input_file=" + filePath);
        assertThat(validationString).contains("initialized local storage reader");
        assertThat(validationString).contains("initializing restore client");
        assertThat(validationString).contains("starting asb validation");
        assertThat(validationString).contains("msg=\"found asb files\" number=1");
        assertThat(validationString).contains("Validation report");
        assertThat(validationString).contains("Start Time:");
        assertThat(validationString).contains("Duration:");
        assertThat(validationString).contains("Records Read:         1000");
        assertThat(validationString).contains("sIndex Read:          0");
        assertThat(validationString).contains("UDFs Read:            0");
        assertThat(validationString).contains("Exit Code: 0");
    }

    @Test
    void validateDirectoryList() {
        int smallBackups = 19;
        String baseDir = "backupDir";
        List<String> backupDirectories = new ArrayList<>();

        // 1. Create 1000-record large backup
        ASBench.on(SOURCE_NAMESPACE, SET1)
                .keys(1_000)
                .batchSize(10)
                .threads(10)
                .recordSize(1)
                .run();
        String largeBackupDir = baseDir + "0";
        BackupResult largeBackup = CliBackup.on(SOURCE_NAMESPACE, largeBackupDir, 1).run();
        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE)).isEqualTo(1000);
        backupDirectories.add(largeBackup.getBackupDir());
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        // 2. Create 19 backups with 1 record each
        for (int i = 1; i <= smallBackups; i++) {
            String dir = baseDir + i;
            String setName = "set" + i;
            String binName = "bin" + i;
            String binValue = "value" + i;

            Key key = new Key(SOURCE_NAMESPACE, setName, "key" + i);
            AerospikeDataUtils.put(key, binName, binValue);

            BackupResult small = CliBackup.on(SOURCE_NAMESPACE, dir, 1).run();
            backupDirectories.add(small.getBackupDir());

            AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        }

        String[] dirList = backupDirectories.toArray(new String[0]);
        String validationString = CliRestore.on().setValidateDirectoryList(dirList).validate();

        // 3. General process assertions
        assertThat(validationString).contains("initializing restore config");
        assertThat(validationString).contains("initializing storage for reader");
        assertThat(validationString).contains("initialized local storage reader");
        assertThat(validationString).contains("initializing restore client");
        assertThat(validationString).contains("starting asb validation");
        assertThat(validationString).contains("msg=\"found asb files\" number=20");

        // 4. Validation summary block
        assertThat(validationString).contains("Validation report");
        assertThat(validationString).contains("Start Time:");
        assertThat(validationString).contains("Duration:");
        assertThat(validationString).contains("Records Read:         1019");
        assertThat(validationString).contains("sIndex Read:          0");
        assertThat(validationString).contains("UDFs Read:            0");
        assertThat(validationString).contains("Exit Code: 0");
    }
}
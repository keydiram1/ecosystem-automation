package api.abs.end2end;

import api.abs.AbsBackupApi;
import api.abs.AbsRestoreApi;
import api.abs.generated.ApiResponse;
import api.abs.generated.model.DtoBackupDetails;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.AerospikeLogger;
import utils.AutoUtils;
import utils.ConfigParametersHandler;
import utils.abs.AbsRunner;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Tag("ABS-E2E")
class RetrieveAsConfigTest extends AbsRunner {
    private static final String ROUTINE_NAME = "noIndexesUdfsRecords";
    private static final String DIRECTORY_PATH = ConfigParametersHandler.getParameter("user.dir") + "/RetrieveAsConfigTest";

    @BeforeAll
    static void setUp() {
        AutoUtils.runBashCommand("rm -rf " + DIRECTORY_PATH);
        AutoUtils.runBashCommand("mkdir " + DIRECTORY_PATH);
    }

    @Test
    void retrieveAsConfig() {
        DtoBackupDetails dtoBackupDetails = AbsBackupApi.startFullBackupSync(ROUTINE_NAME);
        AutoUtils.sleep(2000);
        long created = AbsBackupApi.getCreated(dtoBackupDetails);
        ApiResponse<File> response = AbsRestoreApi.retrieveAsConfFile(ROUTINE_NAME, created);
        AutoUtils.unzipFile(response.getData().toString(), DIRECTORY_PATH);

        int numberOfNodes = srcClient.getNodes().length;
        File[] files = new File(DIRECTORY_PATH).listFiles();
        assertThat(files)
                .as("Retrieved configuration files")
                .hasSize(numberOfNodes);

        for (File file : files) {
            String content = "";
            try {
                content = new String(Files.readAllBytes(Paths.get(file.getPath())));
            } catch (Exception e) {
                AerospikeLogger.info("readAllBytes from the downloaded conf file failed");
                AerospikeLogger.info(e.getMessage());
            }
            AerospikeLogger.info("Content of the file: " + content);
            if (AutoUtils.isRunningOnGCP())
                assertThat(content).as("Configuration file")
                        .contains("source-ns1", "source-ns2", "source-ns3")
                        .containsIgnoringWhitespaces("file    /mnt/disks/data-0/source-ns1.dat")
                        .containsIgnoringWhitespaces("ca-file    /etc/ssl/certs/ca.aerospike.com.pem");
            else
                assertThat(content).as("Configuration file")
                        .contains("source-ns1", "source-ns2", "source-ns3")
                        .containsIgnoringWhitespaces("replication-factor 1", "nsup-period 120");
        }
    }
}
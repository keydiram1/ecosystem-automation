package api.cli.end2end.xdr;

import api.cli.CliBackup;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import utils.aerospike.abs.AerospikeDataUtils;
import utils.cliBackup.CliBackupRunner;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("XDR-CLI-BACKUP")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class XdrManageDbTest extends CliBackupRunner {
    private static final String SOURCE_NAMESPACE = "source-ns9";
    private static final String DC = "DcManageDb";
    private static final String BACKUP_DIR = "XdrManageDbTestDir";
    private static final int LOCAL_PORT = 9095;

    @Test
    void stopXdr() {
        AerospikeDataUtils.startXdr(DC, LOCAL_PORT, SOURCE_NAMESPACE);

        String stopXdrLog = CliBackup.onWithXdr(SOURCE_NAMESPACE, BACKUP_DIR)
                .setLocalPort(LOCAL_PORT)
                .stopXdr(DC);
        assertThat(stopXdrLog).doesNotContain("Error");
        assertThat(stopXdrLog).contains("stopping XDR on the database");

        stopXdrLog = CliBackup.onWithXdr(SOURCE_NAMESPACE, BACKUP_DIR)
                .setLocalPort(LOCAL_PORT)
                .stopXdr(DC);
        assertThat(stopXdrLog).doesNotContain("Error");
    }

    @Test
    void unblockMrt() {
        AerospikeDataUtils.disableMrtWrites(SOURCE_NAMESPACE);

        String unblockMrtLog = CliBackup.onWithXdr(SOURCE_NAMESPACE, BACKUP_DIR)
                .setRemoveFiles()
                .unblockMrtWrites(SOURCE_NAMESPACE);

        assertThat(unblockMrtLog).contains("enabling MRT writes on the database");
    }
}
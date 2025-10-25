package api.cli.negative.xdr;

import api.cli.BackupProcessException;
import api.cli.CliBackup;
import com.aerospike.client.Key;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.aerospike.abs.AerospikeDataUtils;
import utils.cliBackup.CliBackupRunner;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("CLI-BACKUP-NEGATIVE")
@Disabled
class XdrNegativeBackupOptionsTest extends CliBackupRunner {
    private static final String SOURCE_NAMESPACE = "source-ns15";
    private static final String BACKUP_DIR = "XdrNegativeBackupOptionsTestDir";
    private static final String DC = "DcXDRNegativeBackupOptionsTest";

    @Test
    void wrongLocalAddressWithStartTimeout() {
        Key key = new Key(SOURCE_NAMESPACE, "setWrongAddress", "IT1");
        AerospikeDataUtils.put(key, "STRING_BIN", "firstValueCreate");

        assertThatThrownBy(() -> {
            CliBackup.onWithXdr(SOURCE_NAMESPACE, BACKUP_DIR)
                    .setLocalAddress("wrongLocalAddress")
                    .setDc(DC)
                    .setStartTimeout(3000)
                    .run();
        }).isInstanceOf(BackupProcessException.class)
                .hasMessageContaining("xdr scan timed out after: 3s");
    }

    @Test
    void invalidLocalPort() {
        int invalidLocalPort = -1;
        assertThatThrownBy(() -> {
            CliBackup.onWithXdr(SOURCE_NAMESPACE, BACKUP_DIR)
                    .setLocalAddress()
                    .setLocalPort(invalidLocalPort)
                    .setDc(DC)
                    .run();
        }).isInstanceOf(BackupProcessException.class)
                .hasMessageContaining("local port must be between 0 and 65535");
    }

    @Test
    void invalidReadTimeout() {
        int invalidReadTimeout = -1;
        assertThatThrownBy(() -> {
            CliBackup.onWithXdr(SOURCE_NAMESPACE, BACKUP_DIR)
                    .setLocalAddress()
                    //  .setStopXdr()
                    .setReadTimeout(invalidReadTimeout)
                    .setDc(DC)
                    .run();
        }).isInstanceOf(BackupProcessException.class)
                .hasMessageContaining("backup xdr read timeout can't be negative");
    }

    @Test
    void invalidWriteTimeout() {
        int invalidWriteTimeout = -1;
        assertThatThrownBy(() -> {
            CliBackup.onWithXdr(SOURCE_NAMESPACE, BACKUP_DIR)
                    .setLocalAddress()
                    //  .setStopXdr()
                    .setWriteTimeout(invalidWriteTimeout)
                    .setDc(DC)
                    .run();
        }).isInstanceOf(BackupProcessException.class)
                .hasMessageContaining("backup xdr write timeout can't be negative");
    }

    @Test
    void invalidResultsQueueSize() {
        int invalidResultsQueueSize = -1;
        assertThatThrownBy(() -> {
            CliBackup.onWithXdr(SOURCE_NAMESPACE, BACKUP_DIR)
                    .setResultsQueueSize(invalidResultsQueueSize)
                    .setDc(DC)
                    .run();
        }).isInstanceOf(BackupProcessException.class)
                .hasMessageContaining("backup xdr result queue size can't be negative");
    }

    @Test
    void invalidAckQueueSize() {
        int invalidAckQueueSize = -1;
        assertThatThrownBy(() -> {
            CliBackup.onWithXdr(SOURCE_NAMESPACE, BACKUP_DIR)
                    .setAckQueueSize(invalidAckQueueSize)
                    .setDc(DC)
                    .run();
        }).isInstanceOf(BackupProcessException.class)
                .hasMessageContaining("backup xdr ack queue size can't be negative");
    }

    @Test
    void invalidMaxConnections() {
        int invalidMaxConnections = -1;
        assertThatThrownBy(() -> {
            CliBackup.onWithXdr(SOURCE_NAMESPACE, BACKUP_DIR)
                    .setMaxConnections(invalidMaxConnections)
                    .setDc(DC)
                    .run();
        }).isInstanceOf(BackupProcessException.class)
                .hasMessageContaining("backup xdr max connections can't be less than 1");
    }

    @Test
    void invalidInfoPollingPeriod() {
        int invalidPollingPeriod = -1;
        assertThatThrownBy(() -> {
            CliBackup.onWithXdr(SOURCE_NAMESPACE, BACKUP_DIR)
                    .setInfoPollingPeriod(invalidPollingPeriod)
                    .setDc(DC)
                    .run();
        }).isInstanceOf(BackupProcessException.class)
                .hasMessageContaining("backup xdr info poling period can't be negative");
    }

    @Test
    void dcNameTooLong() {
        String dcName = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
        assertThatThrownBy(() -> {
            CliBackup.onWithXdr(SOURCE_NAMESPACE, BACKUP_DIR)
                    .setDc(dcName)
                    .run();
        }).isInstanceOf(BackupProcessException.class)
                .hasMessageContaining("dc name must be less than 32 characters");
    }
}
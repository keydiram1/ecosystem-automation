package api.cli.load.xdr;

import api.cli.CliBackup;
import api.cli.CliRestore;
import utils.ASBench;
import utils.AerospikeLogger;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.abs.AerospikeDataUtils;
import utils.cliBackup.CliBackupRunner;

import static org.assertj.core.api.Assertions.assertThat;


class LoadBackupParent extends CliBackupRunner {
    protected String sourceNamespace;
    protected static final String SET = "LoadSet";
    protected static int numberOfbakcups = 0;
    public static volatile boolean testFinished = false;
    protected String backupDir;
    protected String dc;
    protected int localPort;

    protected void setUpParent() {
        AerospikeDataUtils.truncateSourceNamespace(sourceNamespace);
    }

    protected void testBackupParent() {
        for (int i = 0; i < 400; i++) {
            numberOfbakcups++;
            AerospikeLogger.info("Backup number " + numberOfbakcups);
            AerospikeDataUtils.truncateNamespace(srcClient, sourceNamespace);

            ASBench.on(sourceNamespace, SET).duration(1).run();
            int numberOfRecordsBeforeTruncate = AerospikeCountUtils.getSetObjectCount(srcClient, SET, sourceNamespace);
            assertThat(numberOfRecordsBeforeTruncate).isGreaterThan(100);

            long startTime = System.currentTimeMillis();
            String backupKey = CliBackup.onWithXdr(sourceNamespace, backupDir)
                    .setLocalAddress()
                    .setDc(dc)
                    .setLocalPort(localPort)
                    .run().getBackupDir();
            long duration = System.currentTimeMillis() - startTime;

            double durationInSeconds = duration / 1000;
            AerospikeLogger.info("Backup duration in seconds: " + durationInSeconds);
            assertThat(duration / 1000).isLessThan(20);

            AerospikeDataUtils.truncateSourceNamespace(sourceNamespace);

            startTime = System.currentTimeMillis();
            CliRestore.on(sourceNamespace, backupKey).run();
            duration = System.currentTimeMillis() - startTime;

            durationInSeconds = duration / 1000;
            AerospikeLogger.info("Restore duration in seconds: " + durationInSeconds);
            assertThat(duration / 1000).isLessThan(20);

            assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET, sourceNamespace)).isEqualTo(numberOfRecordsBeforeTruncate);
        }
        testFinished = true;
    }
}

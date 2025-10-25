package utils;

import utils.abs.AbsRunner;
import utils.abs.TlsHandler;
import utils.cliBackup.CliBackupRunner;

import static utils.AutoUtils.isRunningOnGCP;

public class ASBench {
    private Integer duration;
    private final String namespace;
    private final String set;
    private long port = 3000;
    private Long keys;
    private Long throughput;
    private Long startKey;
    private Integer recordSize;
    private String recordType;
    private Integer threads = 20;
    private Integer batchSize = 100;
    private Boolean sendKey = false;

    private ASBench(String namespace, String set) {
        this.namespace = namespace;
        this.set = set;
    }

    public static ASBench on(String namespace, String set) {
        return new ASBench(namespace, set);
    }

    public ASBench threads(int threads) {
        this.threads = threads;
        return this;
    }

    public ASBench batchSize(int batchSize) {
        this.batchSize = batchSize;
        return this;
    }

    public ASBench duration(int duration) {
        this.duration = duration;
        return this;
    }

    public ASBench port(long port) {
        this.port = port;
        return this;
    }

    public ASBench keys(long keys) {
        this.keys = keys;
        return this;
    }

    public ASBench throughput(long throughput) {
        this.throughput = throughput;
        return this;
    }

    public ASBench startKey(long startKey) {
        this.startKey = startKey;
        return this;
    }

    public ASBench recordSize(int recordSize) {
        this.recordSize = recordSize;
        return this;
    }

    public ASBench recordType(String recordType) {
        this.recordType = recordType;
        return this;
    }

    public ASBench sendKey(boolean sendKey) {
        this.sendKey = sendKey;
        return this;
    }

    private String buildCommand() {
        StringBuilder commandBuilder = new StringBuilder("asbench --workload I -U tester -Ppsw ");

        commandBuilder.append("--namespace %s ".formatted(namespace));
        commandBuilder.append("--set %s ".formatted(set));

        if (duration != null) {
            commandBuilder.append("--duration %d ".formatted(duration));
        }
        if (keys != null) {
            commandBuilder.append("--keys %d ".formatted(keys));
        }
        if (throughput != null) {
            commandBuilder.append("--throughput %d ".formatted(throughput));
        }
        if (startKey != null) {
            commandBuilder.append("--start-key %d ".formatted(startKey));
        }
        if (recordSize != null) {
            commandBuilder.append("--object-spec B%d ".formatted(recordSize));
        } else if (recordType != null) {
            commandBuilder.append("--object-spec %s ".formatted(recordType));
        }
        if (threads != null) {
            commandBuilder.append("--threads %d ".formatted(threads));
        }
        if (batchSize != null) {
            commandBuilder.append("--batch-size %d ".formatted(batchSize));
        }
        if (sendKey) {
            commandBuilder.append("--send-key ");
        }
        AerospikeLogger.info("TESTED_PRODUCT=" + ConfigParametersHandler.getParameter("TESTED_PRODUCT"));
        if (isRunningOnGCP()) {
            if (ConfigParametersHandler.getParameter("TESTED_PRODUCT").equals("cli_backup")) {
                AerospikeLogger.info("Creating command for cli on gcp");
                commandBuilder.append("-h %s:%s:%d ".formatted(
                        CliBackupRunner.AEROSPIKE_SOURCE_SERVER_IP,
                        TlsHandler.TLS_NAME, CliBackupRunner.AEROSPIKE_SOURCE_SERVER_PORT));
            } else commandBuilder.append("-h %s:%s:%d ".formatted(
                    utils.ConfigParametersHandler.getParameter("ASDB_DNS_NAME"),
                    TlsHandler.TLS_NAME, AbsRunner.AEROSPIKE_SOURCE_SERVER_PORT));
            commandBuilder.append("--tls-cafile=%s ".formatted(
                    utils.ConfigParametersHandler.getParameter("CA_AEROSPIKE_COM_PEM_PATH")));
            commandBuilder.append("--tls-name=%s ".formatted(utils.init.runners.TlsHandler.getTlsName()));
            commandBuilder.append("--tls-enable");
        } else if (ConfigParametersHandler.getParameter("TLS_ENABLED").equals("true")) {
            commandBuilder.append("-h %s:%s:%d ".formatted(
                    CliBackupRunner.AEROSPIKE_SOURCE_SERVER_IP,
                    TlsHandler.TLS_NAME, CliBackupRunner.AEROSPIKE_SOURCE_SERVER_PORT));
            commandBuilder.append("--tls-cafile=%s ".formatted(
                    ConfigParametersHandler.getParameter("CA_AEROSPIKE_COM_PEM_PATH")));
            commandBuilder.append("--tls-name=%s ".formatted(utils.init.runners.TlsHandler.getTlsName()));
            commandBuilder.append("--tls-enable");
            port = CliBackupRunner.AEROSPIKE_SOURCE_SERVER_PORT;
        } else if (ConfigParametersHandler.getParameter("IS_RUNNING_ON_LOCAL_3_NODES_ENV").equals("true")) {
            commandBuilder.append("-h %s:%s:%d ".formatted(
                    CliBackupRunner.AEROSPIKE_SOURCE_SERVER_IP,
                    TlsHandler.TLS_NAME, CliBackupRunner.AEROSPIKE_SOURCE_SERVER_PORT));
            port = CliBackupRunner.AEROSPIKE_SOURCE_SERVER_PORT;
        }

        commandBuilder.append(" --port %d ".formatted(port));

        return commandBuilder.toString();
    }

    public void run() {
        if (isRunningOnGCP()) {
            port = 4333;
        }

        String command = buildCommand();

        AerospikeLogger.info("Running asbench with command: " + command);

        if (isRunningOnGCP()) {
            AerospikeLogger.info(AutoUtils.runBashCommand(command));
        } else {
            AutoUtils.runBashCommand(command);
        }
    }
}
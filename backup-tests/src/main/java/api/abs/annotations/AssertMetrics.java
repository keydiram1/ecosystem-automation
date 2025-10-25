package api.abs.annotations;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import static api.abs.PrometheusClient.prometheusClient;
import static org.assertj.core.api.Assertions.assertThat;

public class AssertMetrics implements BeforeTestExecutionCallback, AfterTestExecutionCallback {

    private long initialBackupsRunTotal;
    private long initialIncrementalBackupsRunTotal;

    @Override
    public void beforeTestExecution(ExtensionContext context) {
        var metrics = prometheusClient.fetch();
        if (context.getRequiredTestMethod().isAnnotationPresent(BackupsRunIncrease.class)) {
            initialBackupsRunTotal = metrics.backupRunsTotal();
        }

        if (context.getRequiredTestMethod().isAnnotationPresent(IncrementalBackupsRunIncrease.class)) {
            initialIncrementalBackupsRunTotal = metrics.incrementalBackupRunsTotal();
        }
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        var metrics = prometheusClient.fetch();
        if (context.getRequiredTestMethod().isAnnotationPresent(BackupsRunIncrease.class)) {
            long finalValue = metrics.backupRunsTotal();
            assertThat(finalValue)
                    .as("BackupRunsTotal did not increase")
                    .isGreaterThan(initialBackupsRunTotal);
        }

        if (context.getRequiredTestMethod().isAnnotationPresent(IncrementalBackupsRunIncrease.class)) {
            long finalValue = metrics.incrementalBackupRunsTotal();
            assertThat(finalValue)
                    .as("IncrementalBackupRunsTotal did not increase")
                    .isGreaterThan(initialIncrementalBackupsRunTotal);
        }
    }
}
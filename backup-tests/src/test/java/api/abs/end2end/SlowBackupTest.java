package api.abs.end2end;

import api.abs.*;
import api.abs.generated.model.DtoBackupDetails;
import api.abs.generated.model.DtoBackupPolicy;
import api.abs.generated.model.DtoBackupRoutine;
import api.abs.generated.model.DtoMetrics;
import org.assertj.core.data.Offset;
import org.assertj.core.data.Percentage;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import utils.ASBench;
import utils.AerospikeLogger;
import utils.AutoUtils;
import utils.ConfigParametersHandler;
import utils.abs.AbsRunner;
import utils.aerospike.AerospikeCountUtils;
import utils.aerospike.abs.AerospikeDataUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.ToIntFunction;

import static api.abs.AbsBackupApi.parseDate;
import static api.abs.PrometheusClient.prometheusClient;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withinPercentage;

@Tag("ABS-E2E")
@Tag("ABS-SLOW-BACKUP")
@DisabledIfSystemProperty(named = "qa_environment", matches = "GCP")
class SlowBackupTest extends AbsRunner {
    public static final double base64coefficient = 1.34;
    private static final String SET1 = "SetSlowBackupTest";
    private static final String ROUTINE_NAME = "fullBackupSlow";
    private static String SOURCE_NAMESPACE;
    final static DtoBackupRoutine routine = AbsRoutineApi.getRoutine(ROUTINE_NAME);

    @BeforeAll
    static void setUp() {
        SOURCE_NAMESPACE = AbsRoutineApi.getAnyNamespaceForRoutine(routine);
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
    }

    @AfterAll
    static void Cleanup() {
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
    }

    @Test
    void testSlowBackupAndCurrentBackup() {
        final DtoBackupPolicy policy = AbsPolicyApi.getPolicy(routine.getBackupPolicy());
        assertThat(policy.getBandwidth()).isNotNull();
        int keys = 250_000;
        int recordSize = 2500;
        AerospikeDataUtils.createData(SOURCE_NAMESPACE, SET1, keys, recordSize);
        int numberOfRecordsBeforeTruncate = AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE);
        assertThat(numberOfRecordsBeforeTruncate).isEqualTo(keys);

        Instant startTime = Instant.now();
        AbsBackupApi.scheduleFullBackup(ROUTINE_NAME);
        List<DtoMetrics> metrics = new ArrayList<>();
        Awaitility.await()
                .atMost(20, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    var currentBackup = AbsBackupApi.getCurrentBackup(ROUTINE_NAME);
                    assertThat(currentBackup.getFull()).isNotNull();
                    assertThat(currentBackup.getFull().getDoneRecords()).isNotNull();
                    assertThat(currentBackup.getFull().getTotalRecords())
                            .isNotNull().isGreaterThan(0); // wait for counting to be done
                    DtoMetrics metrics1 = currentBackup.getFull().getMetrics();
                    metrics.add(metrics1);
                });

        AbsConfigApi.apply(); // config reload should not affect current backups.
        var currentBackup = AbsBackupApi.getCurrentBackup(ROUTINE_NAME).getFull();

        assertThat(currentBackup).isNotNull();
        assertThat(currentBackup.getTotalRecords()) // total records are calculated approximately
                .isCloseTo(numberOfRecordsBeforeTruncate, Percentage.withPercentage(5));
        int previousDoneRecords = currentBackup.getDoneRecords();
        int previousPercentageDone = currentBackup.getPercentageDone();

        while (currentBackup != null) {
            AutoUtils.sleep(1_000);
            currentBackup = AbsBackupApi.getCurrentBackup(ROUTINE_NAME).getFull();

            if (currentBackup != null) {
                metrics.add(currentBackup.getMetrics());

                int currentDoneRecords = currentBackup.getDoneRecords();
                int currentPercentageDone = currentBackup.getPercentageDone();

                assertThat(currentDoneRecords).isGreaterThanOrEqualTo(previousDoneRecords);
                assertThat(currentPercentageDone).isGreaterThanOrEqualTo(previousPercentageDone);

                previousDoneRecords = currentDoneRecords;
                previousPercentageDone = currentPercentageDone;

                AerospikeLogger.info(currentBackup.toString());

                Map<String, BackupProgress> stringBackupProgressMap = prometheusClient.fetch().backupProgress();
                assertThat(stringBackupProgressMap).as("Prometheus progress map").isNotNull();
                BackupProgress backupProgress = stringBackupProgressMap.get(ROUTINE_NAME);
                assertThat(backupProgress).as("Prometheus current job metric").isNotNull();
                long fullMetrics = backupProgress.full();
                assertThat(fullMetrics).isCloseTo(currentPercentageDone, Offset.offset(20L));
            }
        }

        final DtoBackupDetails backupDetails = AbsBackupApi.waitForFullBackup(ROUTINE_NAME, startTime.toEpochMilli());
        Duration duration = Duration.between(startTime, Instant.now());
        assertThat(backupDetails).isNotNull();
        assertThat(backupDetails.getByteCount()).isNotNull().isPositive();

        assertMajorityWithinTolerance(metrics, DtoMetrics::getKilobytesPerSecond, (int)
                (policy.getBandwidth() * 1024), "kbps");
        assertSummaryWithTolerance(metrics, DtoMetrics::getRecordsPerSecond, keys, "rps");

        AerospikeLogger.info("create backup took " + duration + " milliseconds");
        int expectedDuration = (int) (backupDetails.getByteCount() / policy.getBandwidth() / 1024 / 1024);
        assertThat(duration.toSeconds()).isCloseTo(expectedDuration, withinPercentage(20));

        assertThat(backupDetails.getDuration())
                .isCloseTo((int) duration.toSeconds(), Offset.offset(25));
        assertThat(parseDate(backupDetails.getFinished()) / 1000)
                .isCloseTo(Instant.now().getEpochSecond(), Percentage.withPercentage(10));

        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);
        AbsRestoreApi.restoreFullSync(backupDetails.getKey(), ROUTINE_NAME);

        assertThat(AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE)).isEqualTo(numberOfRecordsBeforeTruncate);
    }

    static void assertMajorityWithinTolerance(
            List<DtoMetrics> metrics,
            ToIntFunction<DtoMetrics> metricExtractor,
            int expectedValue,
            String label
    ) {
        var tolerance = 0.2;
        long withinToleranceCount = metrics.stream()
                .filter(m -> Math.abs(metricExtractor.applyAsInt(m) - expectedValue) <= expectedValue * tolerance)
                .count();

        assertThat(withinToleranceCount)
                .as("At least 50%% of %s values should be within %.0f%% of %d", label, tolerance * 100, expectedValue)
                .isGreaterThanOrEqualTo((long) (metrics.size() * 0.5));
    }

    static void assertSummaryWithTolerance(
            List<DtoMetrics> metrics,
            ToIntFunction<DtoMetrics> metricExtractor,
            int expectedSum,
            String label
    ) {
        var tolerance = 0.2;
        int actualSum = metrics.stream().mapToInt(metricExtractor).sum();
        assertThat(actualSum)
                .as("Sum of all %s readings should be within %.0f%% of %d", label, tolerance * 100, expectedSum)
                .isCloseTo(expectedSum, Percentage.withPercentage(tolerance * 100));
    }

    enum CancelWay {
        DISABLE,
        CANCEL
    }

    @ParameterizedTest
    @EnumSource(CancelWay.class)
    void cancelBackup(CancelWay cancelWay) {
        if (ConfigParametersHandler.getParameter("CONFIGURATION_FILE").equals("http") && cancelWay == CancelWay.DISABLE) {
            return; // http is readonly, cannot disable
        }

        ASBench.on(SOURCE_NAMESPACE, SET1).duration(3).run();
        int numberOfRecordsBeforeTruncate = AerospikeCountUtils.getSetObjectCount(srcClient, SET1, SOURCE_NAMESPACE);
        assertThat(numberOfRecordsBeforeTruncate).isGreaterThan(100);

        Instant startTime = Instant.now();
        AbsBackupApi.scheduleFullBackup(ROUTINE_NAME);
        Awaitility.await()
                .pollInterval(1, TimeUnit.SECONDS)
                .atMost(2, TimeUnit.MINUTES)
                .untilAsserted(() -> {
                    var currentBackup = AbsBackupApi.getCurrentBackup(ROUTINE_NAME);
                    assertThat(currentBackup.getFull()).isNotNull();
                    assertThat(currentBackup.getFull().getDoneRecords()).isNotNull();
                });

        switch (cancelWay) {
            case CANCEL -> AbsBackupApi.cancel(ROUTINE_NAME);
            case DISABLE -> { // when routine is disabled, all current backups are cancelled.
                AbsRoutineApi.disable(ROUTINE_NAME);
                AbsRoutineApi.enable(ROUTINE_NAME);
            }
        }

        Awaitility.await("No backup is running")
                .pollInterval(Duration.ofSeconds(1))
                .atMost(2, TimeUnit.MINUTES) // Cancelling might take long for cloud providers
                .untilAsserted(() -> {
                    var currentBackup = AbsBackupApi.getCurrentBackup(ROUTINE_NAME).getFull();
                    assertThat(currentBackup).isNull();
                });

        List<DtoBackupDetails> backups = AbsBackupApi.getFullBackupsInRange(ROUTINE_NAME, startTime.toEpochMilli(), null);
        assertThat(backups).isEmpty();
    }
}

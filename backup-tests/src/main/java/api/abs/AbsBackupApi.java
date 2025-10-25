package api.abs;

import api.abs.generated.ApiException;
import api.abs.generated.model.DtoBackupDetails;
import api.abs.generated.model.DtoRoutineState;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import utils.AerospikeLogger;
import utils.AutoUtils;
import utils.aerospike.abs.AerospikeDataUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static api.abs.API.backupApi;

@UtilityClass
public class AbsBackupApi {
    private static final Duration DEFAULT_WAIT_TIMEOUT = Duration.ofMinutes(2);

    public DtoRoutineState getCurrentBackup(String routineName) throws ApiException {
        return backupApi.getCurrentBackup(routineName);
    }

    public static Map<String, List<DtoBackupDetails>> getAllFullBackupsInRange(Long from, Long to) {
        return backupApi.getFullBackups(from, to);
    }

    public static Map<String, List<DtoBackupDetails>> getAllIncrementalBackupsInRange(Long from, Long to) {
        return backupApi.getIncrementalBackups(from, to);
    }

    public static List<DtoBackupDetails> getFullBackupsInRange(String routineName, Long from, Long to) {
        List<DtoBackupDetails> backupDetails = backupApi.getFullBackupsForRoutine(routineName, from, to);
        validateBackups(backupDetails);
        return backupDetails;
    }

    public static List<DtoBackupDetails> getIncrementalBackupsInRange(String routineName, Long from, Long to) {
        List<DtoBackupDetails> backupDetails = backupApi.getIncrementalBackupsForRoutine(routineName, from, to);
        validateBackups(backupDetails);
        return backupDetails;
    }

    public static List<DtoBackupDetails> getIncrementalBackups(String routineName) {
        List<DtoBackupDetails> backupDetails = backupApi.getIncrementalBackupsForRoutine(routineName, null, null);
        validateBackups(backupDetails);
        return backupDetails;
    }

    private static void validateBackups(List<DtoBackupDetails> backupDetails) {
        Assertions.assertThat(backupDetails)
                .allMatch(backup -> backup.getCreated() != null && getCreated(backup) > 0, "Created date set");
    }

    public static Optional<DtoBackupDetails> firstFullBackupAfter(String policyName, long backupTime) {
        return getFullBackupsInRange(policyName, backupTime, null).stream()
                .min(Comparator.comparing(AbsBackupApi::getCreated));  // we need the first backup after threshold
    }

    public static DtoBackupDetails firstFullBackupAfter(String routineName, long backupTime, String namespaceName) {
        return allFullBackupsAfter(routineName, backupTime).stream()
                .filter(b -> Objects.equals(b.getNamespace(), namespaceName))
                .findFirst()
                .orElse(null); // Return null if no matching backup is found
    }

    public static List<DtoBackupDetails> allFullBackupsAfter(String routineName, long backupTime) {
        return getFullBackupsInRange(routineName, backupTime, null).stream()
                .filter(b -> getCreated(b) >= backupTime)
                .collect(Collectors.toList());
    }

    public static Optional<DtoBackupDetails> incrementalBackupAfter(String routineName, long backupTime) {
        return getIncrementalBackupsInRange(routineName, backupTime, null).stream()
                .filter(b -> getCreated(b) >= backupTime)
                .min(Comparator.comparing(AbsBackupApi::getCreated));
    }

    public static DtoBackupDetails incrementalBackupAfter(String routineName, long backupTime, String namespaceName) {
        return getIncrementalBackupsInRange(routineName, backupTime, null).stream()
                .filter(b -> Objects.equals(b.getNamespace(), namespaceName))
                .findFirst()
                .orElse(null); // Return null if no matching backup is found
    }

    public static long getCreated(DtoBackupDetails modelBackupDetails) {
        return parseDate(modelBackupDetails.getCreated());
    }

    public static long getFrom(DtoBackupDetails modelBackupDetails) {
        return parseDate(modelBackupDetails.getFrom());
    }

    public static long parseDate(String date) {
        if (date == null || date.isBlank()) {
            return 0;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        OffsetDateTime dateTime = OffsetDateTime.parse(date, formatter);
        return dateTime.toInstant().toEpochMilli();
    }

    @NotNull
    public static DtoBackupDetails waitForFullBackup(String routineName) {
        try {
            return waitForFullBackup(routineName, System.currentTimeMillis());
        } catch (Exception e) {
            throw new WaitForBackupFailedException(e);
        }
    }

    public void scheduleFullBackup(String routineName) {
        scheduleFullBackup(routineName, 1000);
    }

    @SneakyThrows
    public void scheduleFullBackup(String routineName, Integer delay) {
        AerospikeLogger.info("Request full backup for " + routineName);
        backupApi.scheduleFullBackup(routineName, delay);
    }

    public DtoBackupDetails startFullBackupSync(String routineName, Duration timeout) {
        return startFullBackupSync(routineName, timeout, 0);
    }

    public DtoBackupDetails startFullBackupSync(String routineName, Duration timeout, int delay) {
        var currentBackup = AbsBackupApi.getCurrentBackup(routineName);
        long lastFullBackup = parseDate(currentBackup.getLastFull()) + 1;

        scheduleFullBackup(routineName, delay);
        return waitForFullBackup(routineName, lastFullBackup, timeout);
    }

    public DtoBackupDetails startFullBackupSync(String routineName) {
        return startFullBackupSync(routineName, DEFAULT_WAIT_TIMEOUT, 0);
    }

    public DtoBackupDetails startFullBackupSync(String routineName, int delay) {
        return startFullBackupSync(routineName, DEFAULT_WAIT_TIMEOUT, delay);
    }

    public static DtoBackupDetails waitForFullBackup(String routineName, long backupTime) {
        return waitForFullBackup(routineName, backupTime, DEFAULT_WAIT_TIMEOUT);
    }

    public static DtoBackupDetails waitForFullBackup(String routineName, long backupTime, Duration timeout) {
        CompletableFuture<DtoBackupDetails> resultFuture = new CompletableFuture<>();
        AerospikeLogger.info("Wait for full backup for " + routineName + " after time " + Instant.ofEpochMilli(backupTime));
        Awaitility.await()
                .pollInterval(1, TimeUnit.SECONDS)
                .atMost(timeout)
                .until(() -> {
                    Optional<DtoBackupDetails> backup = firstFullBackupAfter(routineName, backupTime);
                    if (backup.isPresent()) {
                        resultFuture.complete(backup.get());
                        AutoUtils.sleepOnCloud(2000);
                        return true;
                    }

                    AbsBackupApi.getCurrentBackup(routineName); // will print current state in log, for debugging

                    return false;
                });
        return resultFuture.join();
    }

    public DtoBackupDetails waitForFullBackup(String routineName, long timeBeforeBackup, String namespace) {
        return Awaitility.await()
                .pollInterval(5, TimeUnit.SECONDS)
                .atMost(DEFAULT_WAIT_TIMEOUT)
                .until(() -> firstFullBackupAfter(routineName, timeBeforeBackup, namespace), Matchers.notNullValue());
    }

    public void waitForFullBackups(String routineName, long timeBeforeBackup, String... namespaces) {
        AerospikeLogger.info("Starting waitForFullBackups for routine " + routineName + " with time " + timeBeforeBackup);
        for (String namespace : namespaces) {
            waitForFullBackup(routineName, timeBeforeBackup, namespace.trim());
        }
    }

    public void waitForAllFullBackups(String routineName, long timeBeforeBackup) {
        String allNamespaces = AerospikeDataUtils.getAllNamespaces();
        String[] namespaces = allNamespaces.split(";");
        AerospikeLogger.info("Starting waitForAllFullBackups for routine " + routineName + " with time " + timeBeforeBackup);
        waitForFullBackups(routineName, timeBeforeBackup, namespaces);
    }

    public static DtoBackupDetails waitForIncrementalBackup(String routineName) {
        var currentBackup = AbsBackupApi.getCurrentBackup(routineName);
        long lastFullBackup = parseDate(currentBackup.getLastFull());
        long lastIncrBackup = parseDate(currentBackup.getLastIncremental());

        return waitForIncrementalBackup(routineName, Math.max(lastFullBackup, lastIncrBackup) + 1);
    }

    public void waitForIncrementalBackup(String routineName, long timeBeforeBackup, String namespace) {
        Awaitility.await("wait for routine %s, namespace %s, after %d".formatted(routineName, namespace, timeBeforeBackup))
                .pollInterval(1, TimeUnit.SECONDS).atMost(DEFAULT_WAIT_TIMEOUT)
                .until(() -> incrementalBackupAfter(routineName, timeBeforeBackup, namespace) != null);
    }

    public void waitForIncrementalBackups(String routineName, long timeBeforeBackup, String... namespaces) {
        AerospikeLogger.info("Starting waitForIncrementalBackups for routine " + routineName + " with time " + timeBeforeBackup);
        for (String namespace : namespaces) {
            waitForIncrementalBackup(routineName, timeBeforeBackup, namespace.trim());
        }
    }

    public static DtoBackupDetails waitForIncrementalBackup(String routineName, long backupTime, Duration timeout) {
        CompletableFuture<DtoBackupDetails> resultFuture = new CompletableFuture<>();
        AerospikeLogger.info("Wait for incremental backup for " + routineName + " after time " + Instant.ofEpochMilli(backupTime));
        Awaitility.await()
                .pollInterval(1, TimeUnit.SECONDS)
                .atMost(timeout)
                .until(() -> {
                    Optional<DtoBackupDetails> modelBackupDetails = incrementalBackupAfter(routineName, backupTime);
                    if (modelBackupDetails.isPresent()) {
                        resultFuture.complete(modelBackupDetails.get());
                        return true;
                    }
                    return false;
                });
        return resultFuture.join();
    }

    public static DtoBackupDetails waitForIncrementalBackup(String routineName, long backupTime) {
        return waitForIncrementalBackup(routineName, backupTime, DEFAULT_WAIT_TIMEOUT);
    }

    public void cancel(String name) {
        backupApi.cancelCurrentBackup(name);
    }
}
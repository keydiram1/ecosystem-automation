package api.abs.end2end.config;

import api.abs.AbsBackupApi;
import api.abs.AbsRoutineApi;
import api.abs.generated.model.DtoBackupDetails;
import lombok.SneakyThrows;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.AutoUtils;
import utils.abs.AbsRunner;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("ABS-E2E")
public class DisableBackupTest extends AbsRunner {
    private static final String ROUTINE_NAME = "everySecondBackup";

    @BeforeEach
    void setUp() {
        AbsRoutineApi.disable(ROUTINE_NAME);
        AutoUtils.sleep(3_000);
    }

    @AfterEach
    void tearDown() {
        AbsRoutineApi.disable(ROUTINE_NAME);
    }

    @SneakyThrows
    @Test
    void enableRoutine() {
        Instant start = Instant.now();

        // routine was disabled before the test
        assertNoNewBackups(start);

        // Enable the routine and wait for a backup to appear
        AbsRoutineApi.enable(ROUTINE_NAME);
        waitForBackup(start);

        // Disable the routine and ensure no more backups are generated
        AbsRoutineApi.disable(ROUTINE_NAME);
        AutoUtils.sleep(3_000);
        Instant afterDisable = Instant.now();
        assertNoNewBackups(afterDisable);
    }

    private static void assertNoNewBackups(Instant since) {
        AutoUtils.sleep(20_000); // Give the system a chance to create a backup (if it were active)
        List<DtoBackupDetails> backups = AbsBackupApi.getFullBackupsInRange(ROUTINE_NAME, since.toEpochMilli(), null);
        assertThat(backups)
                .as("No backups should have been created after routine was disabled")
                .isEmpty();
    }

    private static void waitForBackup(Instant since) {
        Awaitility.await("Waiting for a new backup to appear after enabling the routine")
                .atMost(Duration.ofSeconds(120))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    List<DtoBackupDetails> backups = AbsBackupApi.getFullBackupsInRange(ROUTINE_NAME, since.toEpochMilli(), null);
                    assertThat(backups).as("A backup should be created after enabling the routine").isNotEmpty();
                });
    }
}
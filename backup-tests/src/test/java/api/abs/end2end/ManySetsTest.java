package api.abs.end2end;

import api.abs.AbsBackupApi;
import api.abs.AbsRoutineApi;
import api.abs.generated.model.DtoBackupDetails;
import api.abs.generated.model.DtoBackupRoutine;
import org.junit.jupiter.api.*;
import utils.ASBench;
import utils.AerospikeLogger;
import utils.abs.AbsRunner;
import utils.aerospike.abs.AerospikeDataUtils;

import java.util.List;
import java.util.stream.IntStream;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("ABS-E2E")
class ManySetsTest extends AbsRunner {
    private static final String SOURCE_NAMESPACE = "source-ns8";
    private static final String CLUSTER_NAME = "absDefaultCluster";
    private static final int KEYS = 1000;
    private static final int MAX_SETS = 200;

    private final String ROUTINE_NAME = "ManySetsTest" + randomUUID().toString().substring(0, 8);
    DtoBackupRoutine routine;
    List<String> sets;

    @BeforeAll
    void setUp() {
        sets = IntStream.range(0, MAX_SETS).mapToObj(i -> "manySet" + i).toList();
        sets.forEach(set -> ASBench.on(SOURCE_NAMESPACE, set).keys(KEYS).threads(1).run());
        routine = new DtoBackupRoutine()
                .intervalCron("@yearly")
                .namespaces(List.of(SOURCE_NAMESPACE))
                .sourceCluster(CLUSTER_NAME)
                .storage(absStorageName);
        AbsRoutineApi.createRoutine(ROUTINE_NAME, routine);
    }

    @AfterAll
    void tearDownAll() {
        sets.forEach(set -> AerospikeDataUtils.truncateSourceSet(SOURCE_NAMESPACE, set));
        AbsRoutineApi.deleteRoutine(ROUTINE_NAME);
    }

    @Test
    void manySetsLoop() {
        for (int currentRepetition = 1; currentRepetition <= MAX_SETS; currentRepetition++) {
            AerospikeLogger.info("Starting iteration number " + currentRepetition);
            routine.setList(sets.subList(0, currentRepetition));
            AbsRoutineApi.updateRoutine(ROUTINE_NAME, routine);
            DtoBackupDetails dto = AbsBackupApi.startFullBackupSync(ROUTINE_NAME);
            assertThat(dto.getRecordCount()).isEqualTo((long) KEYS * currentRepetition);
        }
    }
}
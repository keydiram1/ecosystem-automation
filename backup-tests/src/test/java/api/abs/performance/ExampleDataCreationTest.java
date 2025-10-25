package api.abs.performance;

import api.abs.AbsRoutineApi;
import org.junit.jupiter.api.Test;
import utils.ASBench;
import utils.abs.AbsRunner;
import utils.aerospike.abs.AerospikeDataUtils;
import utils.constants.AsDataTypes;

class ExampleDataCreationTest extends AbsRunner {
    private static final String ROUTINE_NAME = "fullBackup1";
    private static String SOURCE_NAMESPACE;

    @Test
    void fastRestore() {
        SOURCE_NAMESPACE = AbsRoutineApi.getAnyNamespaceForRoutine(ROUTINE_NAME);
        AerospikeDataUtils.truncateSourceNamespace(SOURCE_NAMESPACE);

        int keys = 10;

        // Small records - 1kb.
        ASBench.on(SOURCE_NAMESPACE, AsDataTypes.SCALAR_1KB).keys(keys).startKey(0).recordType("I8").run();
        ASBench.on(SOURCE_NAMESPACE, AsDataTypes.SCALAR_1KB).keys(keys).startKey(keys).recordType("S1024").run();
        ASBench.on(SOURCE_NAMESPACE, AsDataTypes.SCALAR_1KB).keys(keys).startKey(keys * 2).recordType("D").run();

        ASBench.on(SOURCE_NAMESPACE, AsDataTypes.COMPLEX_1KB).keys(keys).startKey(0).recordType("B1024").run();
        ASBench.on(SOURCE_NAMESPACE, AsDataTypes.COMPLEX_1KB).keys(keys).startKey(keys).recordType("[256*S2]").run();
        ASBench.on(SOURCE_NAMESPACE, AsDataTypes.COMPLEX_1KB).keys(keys).startKey(keys * 2).recordType("{70*S10:I4}").run();

        ASBench.on(SOURCE_NAMESPACE, AsDataTypes.MIXED_1KB).keys(keys).startKey(0).recordType("I8").run();
        ASBench.on(SOURCE_NAMESPACE, AsDataTypes.MIXED_1KB).keys(keys).startKey(keys).recordType("S1024").run();
        ASBench.on(SOURCE_NAMESPACE, AsDataTypes.MIXED_1KB).keys(keys).startKey(keys * 2).recordType("D").run();
        ASBench.on(SOURCE_NAMESPACE, AsDataTypes.MIXED_1KB).keys(keys).startKey(keys * 3).recordType("B1024").run();
        ASBench.on(SOURCE_NAMESPACE, AsDataTypes.MIXED_1KB).keys(keys).startKey(keys * 4).recordType("[256*S2]").run();
        ASBench.on(SOURCE_NAMESPACE, AsDataTypes.MIXED_1KB).keys(keys).startKey(keys * 5).recordType("{70*S10:I4}").run();

        // Medium records - 3kb.
        ASBench.on(SOURCE_NAMESPACE, AsDataTypes.SCALAR_3KB).keys(keys).startKey(0).recordType("I8").run();
        ASBench.on(SOURCE_NAMESPACE, AsDataTypes.SCALAR_3KB).keys(keys).startKey(keys).recordType("S3072").run();
        ASBench.on(SOURCE_NAMESPACE, AsDataTypes.SCALAR_3KB).keys(keys).startKey(keys * 2).recordType("D").run();

        ASBench.on(SOURCE_NAMESPACE, AsDataTypes.COMPLEX_3KB).keys(keys).startKey(0).recordType("B3072").run();
        ASBench.on(SOURCE_NAMESPACE, AsDataTypes.COMPLEX_3KB).keys(keys).startKey(keys).recordType("[768*S2]").run();
        ASBench.on(SOURCE_NAMESPACE, AsDataTypes.COMPLEX_3KB).keys(keys).startKey(keys * 2).recordType("{210*S10:I4}").run();

        ASBench.on(SOURCE_NAMESPACE, AsDataTypes.MIXED_3KB).keys(keys).startKey(0).recordType("I8").run();
        ASBench.on(SOURCE_NAMESPACE, AsDataTypes.MIXED_3KB).keys(keys).startKey(keys).recordType("S3072").run();
        ASBench.on(SOURCE_NAMESPACE, AsDataTypes.MIXED_3KB).keys(keys).startKey(keys * 2).recordType("D").run();
        ASBench.on(SOURCE_NAMESPACE, AsDataTypes.MIXED_3KB).keys(keys).startKey(keys * 3).recordType("B3072").run();
        ASBench.on(SOURCE_NAMESPACE, AsDataTypes.MIXED_3KB).keys(keys).startKey(keys * 4).recordType("[768*S2]").run();
        ASBench.on(SOURCE_NAMESPACE, AsDataTypes.MIXED_3KB).keys(keys).startKey(keys * 5).recordType("{210*S10:I4}").run();

        // Large records - 100kb.
        ASBench.on(SOURCE_NAMESPACE, AsDataTypes.SCALAR_100KB).keys(keys).startKey(0).recordType("I8").run();
        ASBench.on(SOURCE_NAMESPACE, AsDataTypes.SCALAR_100KB).keys(keys).startKey(keys).recordType("S102400").run();
        ASBench.on(SOURCE_NAMESPACE, AsDataTypes.SCALAR_100KB).keys(keys).startKey(keys * 2).recordType("D").run();

        ASBench.on(SOURCE_NAMESPACE, AsDataTypes.COMPLEX_100KB).keys(keys).startKey(0).recordType("B102400").run();
        ASBench.on(SOURCE_NAMESPACE, AsDataTypes.COMPLEX_100KB).keys(keys).startKey(keys).recordType("[25600*S2]").run();
        ASBench.on(SOURCE_NAMESPACE, AsDataTypes.COMPLEX_100KB).keys(keys).startKey(keys * 2).recordType("{7000*S10:I4}").run();

        ASBench.on(SOURCE_NAMESPACE, AsDataTypes.MIXED_100KB).keys(keys).startKey(0).recordType("I8").run();
        ASBench.on(SOURCE_NAMESPACE, AsDataTypes.MIXED_100KB).keys(keys).startKey(keys).recordType("S102400").run();
        ASBench.on(SOURCE_NAMESPACE, AsDataTypes.MIXED_100KB).keys(keys).startKey(keys * 2).recordType("D").run();
        ASBench.on(SOURCE_NAMESPACE, AsDataTypes.MIXED_100KB).keys(keys).startKey(keys * 3).recordType("B102400").run();
        ASBench.on(SOURCE_NAMESPACE, AsDataTypes.MIXED_100KB).keys(keys).startKey(keys * 4).recordType("[25600*S2]").run();
        ASBench.on(SOURCE_NAMESPACE, AsDataTypes.MIXED_100KB).keys(keys).startKey(keys * 5).recordType("{7000*S10:I4}").run();
    }
}
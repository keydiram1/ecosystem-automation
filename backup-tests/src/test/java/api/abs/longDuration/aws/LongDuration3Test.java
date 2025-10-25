package api.abs.longDuration.aws;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.AutoUtils;
import utils.ConfigParametersHandler;

@Tag("ABS-LONG-DURATION-TEST")
class LongDuration3Test extends LongDurationParent {

    @BeforeEach
    void setUp() {
        this.routineName = "fullBackup3";
        setUpParent();
    }

    @Test
    void restoreFullInLoop() {
        if(TEST_DURATION.equals("long"))
            AutoUtils.sleep(120_000);
        restoreFullInLoopParent();
    }
}
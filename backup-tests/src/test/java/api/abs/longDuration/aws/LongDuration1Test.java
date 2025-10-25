package api.abs.longDuration.aws;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("ABS-LONG-DURATION-TEST")
class LongDuration1Test extends LongDurationParent {

    @BeforeEach
    void setUp() {
        this.routineName = "fullBackup1";
        setUpParent();
    }

    @Test
    void restoreFullInLoop() {
        restoreFullInLoopParent();
    }
}
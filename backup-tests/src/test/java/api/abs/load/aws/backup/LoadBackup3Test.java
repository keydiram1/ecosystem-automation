package api.abs.load.aws.backup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("ABS-LOAD-TEST")
class LoadBackup3Test extends LoadBackupParent {

    @BeforeEach
    void setUp() {
        this.routineName = "fullBackup4";
        setUpParent();
    }

    @Test
    void runBackup() {
        testBackupParent();
    }
}
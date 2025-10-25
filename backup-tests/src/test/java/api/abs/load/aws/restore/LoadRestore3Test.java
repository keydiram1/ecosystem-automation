package api.abs.load.aws.restore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("ABS-LOAD-TEST")
class LoadRestore3Test extends LoadRestoreParent {

    @BeforeEach
    void setUp() {
        this.routineName = "fullBackup5";
        setUpParent();
    }

    @Test
    void runRestore() {
        restoreBackupParent();
    }
}
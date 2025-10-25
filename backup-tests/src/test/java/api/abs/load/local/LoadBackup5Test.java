package api.abs.load.local;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("ABS-LOCAL-LOAD-TEST")
class LoadBackup5Test extends LoadBackupParent {

    @BeforeEach
    void setUp() {
        this.routineName = "fullBackup5";
        setUpParent();
    }

    @Test
    void takeBackupAndRestore() {
        testBackupParent(3003, srcClient2);
    }
}
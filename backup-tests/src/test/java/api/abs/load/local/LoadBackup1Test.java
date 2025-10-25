package api.abs.load.local;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("ABS-LOCAL-LOAD-TEST")
class LoadBackup1Test extends LoadBackupParent {

    @BeforeEach
    void setUp() {
        this.routineName = "fullBackup1";
        setUpParent();
    }

    @Test
    void takeBackupAndRestore() {
        testBackupParent(AEROSPIKE_SOURCE_SERVER_PORT, srcClient);
    }
}
package api.abs.load.local;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("ABS-LOCAL-LOAD-TEST")
class LoadBackup2Test extends LoadBackupParent {

    @BeforeEach
    void setUp() {
        this.routineName = "fullBackup2";
        setUpParent();
    }

    @Test
    void takeBackupAndRestore() {
        testBackupParent(AEROSPIKE_SOURCE_SERVER_PORT, srcClient);
    }
}
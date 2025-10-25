package api.cli.load.scan;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

//@Tag("CLI-LOAD-TEST")
class LoadBackup3Test extends LoadBackupParent {

    @BeforeEach
    void setUp() {
        this.sourceNamespace = "source-ns3";
        this.backupDir = "loadDir3";
        setUpParent();
    }

    @Test
    void takeBackupAndRestore() {
        testBackupParent();
    }
}
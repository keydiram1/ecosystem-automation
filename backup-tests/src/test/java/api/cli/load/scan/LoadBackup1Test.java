package api.cli.load.scan;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

//@Tag("CLI-LOAD-TEST")
class LoadBackup1Test extends LoadBackupParent {

    @BeforeEach
    void setUp() {
        this.sourceNamespace = "source-ns1";
        this.backupDir = "loadDir1";
        setUpParent();
    }

    @Test
    void takeBackupAndRestore() {
        testBackupParent();
    }
}
package api.cli.load.scan;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

//@Tag("CLI-LOAD-TEST")
class LoadBackup5Test extends LoadBackupParent {

    @BeforeEach
    void setUp() {
        this.sourceNamespace = "source-ns5";
        this.backupDir = "loadDir5";
        setUpParent();
    }

    @Test
    void takeBackupAndRestore() {
        testBackupParent();
    }
}
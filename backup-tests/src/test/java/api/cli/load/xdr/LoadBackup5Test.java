package api.cli.load.xdr;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

//@Tag("CLI-XDR-LOAD-TEST")
class LoadBackup5Test extends LoadBackupParent {

    @BeforeEach
    void setUp() {
        this.dc = "LoadBackup5TestDC";
        this.localPort = 8085;
        this.sourceNamespace = "source-ns5";
        this.backupDir = "loadDir5";
        setUpParent();
    }

    @Test
    void takeBackupAndRestore() {
        testBackupParent();
    }
}
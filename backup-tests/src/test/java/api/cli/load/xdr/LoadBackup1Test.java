package api.cli.load.xdr;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

//@Tag("CLI-XDR-LOAD-TEST")
class LoadBackup1Test extends LoadBackupParent {

    @BeforeEach
    void setUp() {
        this.dc = "LoadBackup1TestDC";
        this.localPort = 8081;
        this.sourceNamespace = "source-ns1";
        this.backupDir = "loadDir1";
        setUpParent();
    }

    @Test
    void takeBackupAndRestore() {
        testBackupParent();
    }
}
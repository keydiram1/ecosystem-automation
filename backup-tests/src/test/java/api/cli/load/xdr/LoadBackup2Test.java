package api.cli.load.xdr;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

//@Tag("CLI-XDR-LOAD-TEST")
class LoadBackup2Test extends LoadBackupParent {

    @BeforeEach
    void setUp() {
        this.dc = "LoadBackup2TestDC";
        this.localPort = 8082;
        this.sourceNamespace = "source-ns2";
        this.backupDir = "loadDir2";
        setUpParent();
    }

    @Test
    void takeBackupAndRestore() {
        testBackupParent();
    }
}
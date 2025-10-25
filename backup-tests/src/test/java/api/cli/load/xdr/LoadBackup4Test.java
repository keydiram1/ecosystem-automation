package api.cli.load.xdr;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

//@Tag("CLI-XDR-LOAD-TEST")
class LoadBackup4Test extends LoadBackupParent {

    @BeforeEach
    void setUp() {
        this.dc = "LoadBackup4TestDC";
        this.localPort = 8084;
        this.sourceNamespace = "source-ns4";
        this.backupDir = "loadDir4";
        setUpParent();
    }

    @Test
    void takeBackupAndRestore() {
        testBackupParent();
    }
}
package api.cli.load.xdr;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

//@Tag("CLI-XDR-LOAD-TEST")
class LoadBackup3Test extends LoadBackupParent {

    @BeforeEach
    void setUp() {
        this.dc = "LoadBackup3TestDC";
        this.localPort = 8083;
        this.sourceNamespace = "source-ns3";
        this.backupDir = "loadDir3";
        setUpParent();
    }

    @Test
    void takeBackupAndRestore() {
        testBackupParent();
    }
}
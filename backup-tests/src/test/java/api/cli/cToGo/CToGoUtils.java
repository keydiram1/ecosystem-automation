package api.cli.cToGo;

import api.cli.CliBackup;
import api.cli.CliRestore;
import utils.ASBackup;
import utils.ASRestore;
import utils.ConfigParametersHandler;

import static org.assertj.core.api.Assertions.fail;

public class CToGoUtils {

    private static final String BACKUP_METHOD = ConfigParametersHandler.getParameter("BACKUP_METHOD");
    private static final String RESTORE_METHOD = System.getProperty("RESTORE_METHOD");
    private static final String FAIL_MESSAGE = "Didn't select backup/restore method";

    public static String runBackupByConfiguration(String backupFilesPath, String namespace) {
        if (BACKUP_METHOD.equals("go_backup")) {
            return CliBackup.on(namespace, backupFilesPath).run().getBackupDir();
        } else if (BACKUP_METHOD.equals("c_backup")) {
            ASBackup.on(namespace).run();
            return ASBackup.asbackupLocalDefaultDirectory;
        } else
            return fail(FAIL_MESSAGE);
    }

    public static void runRestoreByConfigurationWithEncryption(String backupFilesPath, String namespace) {
        if (RESTORE_METHOD.equals("go_restore"))
            CliRestore.on(namespace, backupFilesPath).setEncryptionKeyFile().run();
        else if (RESTORE_METHOD.equals("c_restore"))
            ASRestore.on(namespace, backupFilesPath).run();
        else
            fail(FAIL_MESSAGE);
    }

    public static String runBackupByConfiguration(String namespace, String compressionMode, int compressionLevel, String encryptionMode) {
        if (BACKUP_METHOD.equals("go_backup")) {
            return CliBackup.on(namespace, "cToGoBackup").setEncryptionKeyFile().setEncryptionMode(encryptionMode)
                    .setCompressMode(compressionMode).setCompressLevel(compressionLevel).run().getBackupDir();
        } else if (BACKUP_METHOD.equals("c_backup")) {
            ASBackup.on(namespace).setCompressionMode(compressionMode).setCompressionLevel(compressionLevel).encryptionKeyFile("encryptionKey").
                    encryptionMode(encryptionMode).run();
            return ASBackup.asbackupLocalDefaultDirectory;
        } else
            return fail(FAIL_MESSAGE);
    }

    public static void runRestoreByConfigurationWithEncryption(String backupFilesPath, String namespace, String compressionMode, String encryptionMode) {
        if (RESTORE_METHOD.equals("go_restore"))
            CliRestore.on(namespace, backupFilesPath).setEncryptionKeyFile().setEncryptionMode(encryptionMode).setCompressMode(compressionMode).run();
        else if (RESTORE_METHOD.equals("c_restore"))
            ASRestore.on(namespace, backupFilesPath).encryptionKeyFile("encryptionKey").encryptionMode(encryptionMode).setCompressionMode(compressionMode).run();
        else
            fail(FAIL_MESSAGE);
    }
}

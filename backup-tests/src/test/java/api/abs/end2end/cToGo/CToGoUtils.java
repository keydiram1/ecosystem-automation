package api.abs.end2end.cToGo;

import api.abs.AbsBackupApi;
import api.abs.AbsRestoreApi;
import api.abs.JobID;
import api.abs.generated.model.DtoRestorePolicy;
import utils.ASBackup;
import utils.ASRestore;
import utils.ConfigParametersHandler;

import static org.assertj.core.api.Assertions.fail;

public class CToGoUtils {

    public static String runBackupByConfiguration(String routine, String namespace) {
        return runBackupByConfiguration(routine, namespace, null, 0, null, null);
    }

    public static String runBackupByConfiguration(String routine, String namespace, String compressionMode, int compressionLevel, String encryptionMode,
                                                  String encryptionKeyFile) {
        if (ConfigParametersHandler.getParameter("BACKUP_METHOD").equals("backup_service")) {
            ASBackup.cleanDefaultBackupDirectory();
            return AbsBackupApi.startFullBackupSync(routine).getKey();
        } else if (ConfigParametersHandler.getParameter("BACKUP_METHOD").equals("asbackup"))
            return ASBackup.runBackup(namespace, compressionMode, compressionLevel, encryptionMode, encryptionKeyFile);
        else
            return fail("No backup method selected");
    }

    public static void runRestoreByConfiguration(String backupFilesPath, String routine, String namespace) {
        runRestoreByConfiguration(null, backupFilesPath, routine, namespace, null, null, null);
    }

    public static void runRestoreByConfiguration(DtoRestorePolicy restorePolicy, String backupFilesPath, String routine, String namespace, String compressionMode, String encryptionMode,
                                                 String encryptionKeyFile) {
        if (ConfigParametersHandler.getParameter("RESTORE_METHOD").equals("backup_service"))
            if (restorePolicy == null)
                AbsRestoreApi.restoreFullSync(backupFilesPath, routine);
            else {
                JobID jobID = AbsRestoreApi.restoreFull(backupFilesPath, routine, restorePolicy);
                AbsRestoreApi.waitForRestore(jobID);
            }
        else if (ConfigParametersHandler.getParameter("RESTORE_METHOD").equals("asrestore"))
            ASRestore.runRestore("/data/" + backupFilesPath, namespace, compressionMode, encryptionMode, encryptionKeyFile);
        else
            fail("Backup method has not been selected");
    }
}

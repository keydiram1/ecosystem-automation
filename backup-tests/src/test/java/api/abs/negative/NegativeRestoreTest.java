package api.abs.negative;

import api.abs.AbsBackupApi;
import api.abs.AbsRestoreApi;
import api.abs.AbsRoutineApi;
import api.abs.JobID;
import api.abs.generated.model.DtoCompressionPolicy;
import api.abs.generated.model.DtoEncryptionPolicy;
import api.abs.generated.model.DtoRestorePolicy;
import com.aerospike.client.Key;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import utils.abs.AbsRunner;
import utils.aerospike.abs.AerospikeDataUtils;

@Tag("ABS-NEGATIVE-TESTS")
@DisabledIfSystemProperty(named = "qa_environment", matches = "GCP")
class NegativeRestoreTest extends AbsRunner {
    private static final String ROUTINE_NAME = "fullBackupEncryptedCompressed128";

    @Test
    void restoreWithoutCompressionMode() {
        var backupKey = AbsBackupApi.startFullBackupSync(ROUTINE_NAME);

        DtoEncryptionPolicy encryptionPolicy = new DtoEncryptionPolicy()
                .keyFile("/encryptionKey")
                .mode(DtoEncryptionPolicy.ModeEnum.AES128);
        DtoRestorePolicy restorePolicy = new DtoRestorePolicy()
                .encryption(encryptionPolicy);
        JobID jobId = AbsRestoreApi.restoreFull(backupKey.getKey(), ROUTINE_NAME, restorePolicy);
        AbsRestoreApi.waitForRestoreFail(jobId);
    }

    @Test
    void restoreWithoutEncryptionMode() {
        String namespace = AbsRoutineApi.getAnyNamespaceForRoutine(ROUTINE_NAME);
        AerospikeDataUtils.put(new Key(namespace, "set", "key"), "bin", "new value");
        var backupKey = AbsBackupApi.startFullBackupSync(ROUTINE_NAME);

        DtoCompressionPolicy compressionPolicy = new DtoCompressionPolicy()
                .mode(DtoCompressionPolicy.ModeEnum.ZSTD);
        DtoRestorePolicy restorePolicy = new DtoRestorePolicy()
                .compression(compressionPolicy);
        JobID jobId = AbsRestoreApi.restoreFull(backupKey.getKey(), ROUTINE_NAME, restorePolicy);
        AbsRestoreApi.waitForRestoreFail(jobId);
    }

    @Test
    void invalidEncryptionKeySecret() {
        var backupKey = AbsBackupApi.startFullBackupSync(ROUTINE_NAME);
        DtoEncryptionPolicy encryptionPolicy = new DtoEncryptionPolicy()
                .keySecret("invalidKey")
                .mode(DtoEncryptionPolicy.ModeEnum.AES128);
        DtoCompressionPolicy compressionPolicy = new DtoCompressionPolicy()
                .mode(DtoCompressionPolicy.ModeEnum.ZSTD);
        DtoRestorePolicy restorePolicy = new DtoRestorePolicy()
                .encryption(encryptionPolicy)
                .compression(compressionPolicy);
        JobID jobId = AbsRestoreApi.restoreFull(backupKey.getKey(), ROUTINE_NAME, restorePolicy);
        AbsRestoreApi.waitForRestoreFail(jobId);
    }

    @Test
    void invalidEncryptionKeyFile() {
        var backupKey = AbsBackupApi.startFullBackupSync(ROUTINE_NAME);
        DtoEncryptionPolicy encryptionPolicy = new DtoEncryptionPolicy()
                .keyFile("/etc/aerospike-backup-service/aerospike-backup-service.yml")
                .mode(DtoEncryptionPolicy.ModeEnum.AES128);
        DtoCompressionPolicy compressionPolicy = new DtoCompressionPolicy()
                .mode(DtoCompressionPolicy.ModeEnum.ZSTD);
        DtoRestorePolicy restorePolicy = new DtoRestorePolicy()
                .encryption(encryptionPolicy)
                .compression(compressionPolicy);
        JobID jobId = AbsRestoreApi.restoreFull(backupKey.getKey(), ROUTINE_NAME, restorePolicy);
        AbsRestoreApi.waitForRestoreFail(jobId);
    }

    @Test
    void invalidEncryptionKeyEnv() {
        var backupKey = AbsBackupApi.startFullBackupSync(ROUTINE_NAME);
        DtoEncryptionPolicy encryptionPolicy = new DtoEncryptionPolicy()
                .keyEnv("PATH")
                .mode(DtoEncryptionPolicy.ModeEnum.AES128);
        DtoCompressionPolicy compressionPolicy = new DtoCompressionPolicy()
                .mode(DtoCompressionPolicy.ModeEnum.ZSTD);
        DtoRestorePolicy restorePolicy = new DtoRestorePolicy()
                .encryption(encryptionPolicy)
                .compression(compressionPolicy);
        JobID jobId = AbsRestoreApi.restoreFull(backupKey.getKey(), ROUTINE_NAME, restorePolicy);
        AbsRestoreApi.waitForRestoreFail(jobId);
    }

    @Test
    void wrongEncryptionKey() {
        var backupKey = AbsBackupApi.startFullBackupSync(ROUTINE_NAME);
        DtoEncryptionPolicy encryptionPolicy = new DtoEncryptionPolicy()
                .keySecret("""
                        -----BEGIN PRIVATE KEY-----
                        MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDRXoGVkfDpSILe
                        Ztt7+jrxhJo1QYqI3jQhimVHp9XSeMXMDGJl6KDWRC7H+3wNHNXY3T35owm938JF
                        gakQuH1pJxR23YMIMJE+OD9m/CbFqMVGjfNUMCtLWrW/bKf89dDVgNPtr9emyW3l
                        tP0gU8Xd6JEFLLgqwbqoono1gqb8F2uuG2nq1HszvxIN1bpF5/9IjqVaYV7FPDkY
                        d+0gLa/TDJow6PNJ3GxOdjBIB39ygkLN8vQJla3g+wVb1PrBVAc6kJLjDfnKIKCs
                        UNcXeed/n09VwGBR9JLfT8U2vGG4OoCRkKZvMH6o87qn2g3dSpUxkhqxq/VwM44Y
                        CIMgvu6lAgMBAAECggEAJ4nx7eWkOeIEOOzyySx94it5FdKdT3CrJqRksQ4MjtVZ
                        AUpbcMRoT16L+R9Qk4ddCTnq4NB93/o4TXkJjQtSP7/uGO6HNs45N6dWreOzjMNt
                        EgBvsffwzSfBx/dEmDxx5kgQfcQl9Sz4EJfH78lyRhCMBo6l7wkr73nTm3RjhZqt
                        K/8Pe8Kl2VGbVgwQXGa3hCHh7OlF4jd/WNfUmm+4F4dcbHiwf/fctP64iITtAnsN
                        5cr/5/07MnrfBghEiE5kU1AHWCd+WOA4YQ5XxPX5OVH+NQA7f+t6gJrz+NNWOTaG
                        leFTplMxHffaTS9Ow6itezzsaJPD9fN/uMHMxdZWMQKBgQDwzfF4Y41eOhC9BHhc
                        zBGl9nux2HDvJlz8zYh+H32cPxPQCESnKmkg0ryxmb6RtRiyr3IXGNH/yxqOA3pl
                        iHxf0e1OdyRaFcnaStr0zM3gy58AvieBlnO3ct0temxgL4Cr4EIfLOOy8Iuc8F5E
                        A7kCP1WidkUvDXaW4KxUd2s8nQKBgQDelL5tNDTon7ccBvzyZ2/cDymG6JoX6KUu
                        /FIZX876c6SljQfvPIkSZb0VP2WYuqZQwXpCt9aSQrgXGlC/A2VERlNPAhWb7qg5
                        g7WN1MbEafzx+mMMoLz8+NAIYHpYirTZng/QEJahOzm/Gw5JWVZPdxqrGu8K8IUa
                        n2GcfhgnqQKBgQDmWds38uN0087XQcNx4bSwMg05n3bJDsW8/1AmdjFOmfh19LLX
                        xnYyc+UVb6Eg+T/bDoHxHkkkjEGmwCUBo4J2lvaPLOGVW2pwa1LxAkmfVKmRqFVa
                        XXXscvCpZIbvrAFGaYxXDjXzIrArPLSwE2+TesqMUt7zl8ltoxa25lWq8QKBgQDY
                        YEUWqC/fDJTXXDu98qwdg2fdTHWwVGtwV74ACNGPS6h9f7J88Z5XEixUYYMEjD5u
                        NwgBn/Gp0c5gNybT6hsh2jPWlLIpGkLTVqTCMlzgSCCro7tyZTgTvFWQLyoAtSbG
                        fw/kNpwYNRkYHNcwmvZ63dBBEjfbrKHYlaQwm6wjsQKBgEZ1QhkU4in28NY2Rzs+
                        9E+QkwDI+DeApeAI5OSaVj0M5+Pj5NqHdepdA3BL7eEbo+aaXUMRIOEmIT1zyFJa
                        7isw7CuvGWj6TA/7Zpcd47iH33Zo0jd82ZHbctr5jV291hz3aacDUidIWqfqN58V
                        /dkCIQFUt19JQ18AXvPg9B0W
                        -----END PRIVATE KEY-----""")
                .mode(DtoEncryptionPolicy.ModeEnum.AES128);
        DtoCompressionPolicy compressionPolicy = new DtoCompressionPolicy()
                .mode(DtoCompressionPolicy.ModeEnum.ZSTD);
        DtoRestorePolicy restorePolicy = new DtoRestorePolicy()
                .encryption(encryptionPolicy)
                .compression(compressionPolicy);
        JobID jobId = AbsRestoreApi.restoreFull(backupKey.getKey(), ROUTINE_NAME, restorePolicy);
        AbsRestoreApi.waitForRestoreFail(jobId);
    }

    @Test
    void wrongEncryptionMode() {
        var backupKey = AbsBackupApi.startFullBackupSync(ROUTINE_NAME);
        DtoEncryptionPolicy encryptionPolicy = new DtoEncryptionPolicy()
                .keyFile("/encryptionKey")
                .mode(DtoEncryptionPolicy.ModeEnum.AES256);
        DtoCompressionPolicy compressionPolicy = new DtoCompressionPolicy()
                .mode(DtoCompressionPolicy.ModeEnum.ZSTD);
        DtoRestorePolicy restorePolicy = new DtoRestorePolicy()
                .encryption(encryptionPolicy)
                .compression(compressionPolicy);
        JobID jobId = AbsRestoreApi.restoreFull(backupKey.getKey(), ROUTINE_NAME, restorePolicy);
        AbsRestoreApi.waitForRestoreFail(jobId);
    }
}
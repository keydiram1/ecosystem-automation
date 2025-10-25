package api.abs.end2end.config;

import api.abs.AbsStorageApi;
import api.abs.generated.model.DtoLocalStorage;
import api.abs.generated.model.DtoS3Storage;
import api.abs.generated.model.DtoStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.assertj.core.api.Assertions.assertThat;

@DisabledIfSystemProperty(named = "qa_environment", matches = "GCP")
@Tag("ABS-SEQUENTIAL-TESTS-2")
@Execution(ExecutionMode.SAME_THREAD)
class StorageTest extends ConfigCRUD {

    private static final String STORAGE_NAME = "StorageTestStorage";
    private final DtoStorage storageAllFields = new DtoStorage()
            .s3Storage(new DtoS3Storage()
                    .s3EndpointOverride("s3EndpointOverride")
                    .bucket("bucket")
                    .path("/")
                    .s3LogLevel(DtoS3Storage.S3LogLevelEnum.DEBUG)
                    .s3Profile("s3Profile")
                    .s3Region("s3Region")
            );

    @BeforeEach
    public void setUp() {
        if (AbsStorageApi.getAllStorage().containsKey(STORAGE_NAME)) {
            AbsStorageApi.deleteStorage(STORAGE_NAME);
        }
    }

    @Test
    void deleteStorage() {
        AbsStorageApi.createStorage(STORAGE_NAME,
                new DtoStorage().localStorage(
                        new DtoLocalStorage().path("testPath")));
        assertThat(AbsStorageApi.getAllStorage().get(STORAGE_NAME)).isNotNull();
        AbsStorageApi.deleteStorage(STORAGE_NAME);
        assertThat(AbsStorageApi.getAllStorage().get(STORAGE_NAME)).isNull();
    }

    @Test
    void createStorage() {
        AbsStorageApi.createStorage(STORAGE_NAME, storageAllFields);
        assertThat(AbsStorageApi.getAllStorage().get(STORAGE_NAME))
                .isNotNull()
                .isEqualTo(storageAllFields)
                .isEqualTo(AbsStorageApi.getStorage(STORAGE_NAME));
    }

    @Test
    void updateStorage() {
        AbsStorageApi.createStorage(STORAGE_NAME,
                new DtoStorage().localStorage(
                        new DtoLocalStorage().path("testPath")));
        AbsStorageApi.updateStorage(STORAGE_NAME, storageAllFields);

        assertThat(AbsStorageApi.getStorage(STORAGE_NAME))
                .isNotNull()
                .isEqualTo(storageAllFields);
    }
}

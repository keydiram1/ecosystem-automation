package utils.storage;

import api.abs.generated.model.DtoGcpStorage;
import com.google.api.gax.paging.Page;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageClass;
import com.google.cloud.storage.StorageOptions;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import utils.AerospikeLogger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@UtilityClass
public class GcpStorageUtils {

    public StorageClass getGcpStorageClass(String objectPath, DtoGcpStorage storage) throws IOException {
        // Load credentials from key file content
        ServiceAccountCredentials credentials = ServiceAccountCredentials.fromStream(
                new ByteArrayInputStream(storage.getKey().getBytes(StandardCharsets.UTF_8))
        );

        // Create a Storage client
        Storage gcpStorage = StorageOptions.newBuilder()
                .setCredentials(credentials)
                .build()
                .getService();

        // Retrieve the blob (object)
        Blob blob = gcpStorage.get(storage.getBucketName(), objectPath);
        if (blob == null) {
            throw new IllegalArgumentException("Object not found: " + objectPath);
        }

        return blob.getStorageClass(); // Returns "STANDARD", "NEARLINE", "COLDLINE", "ARCHIVE"
    }

    @SneakyThrows
    public List<String> listGcpObjects(String path, DtoGcpStorage storage) {
        AerospikeLogger.info("List Gcp objects in " + path);
        ServiceAccountCredentials credentials = ServiceAccountCredentials.fromStream(
                new ByteArrayInputStream(storage.getKey().getBytes(StandardCharsets.UTF_8))
        );

        Storage gcpStorage = StorageOptions.newBuilder()
                .setCredentials(credentials)
                .build()
                .getService();

        String prefix = path.endsWith("/") ? path : path + "/";

        Page<Blob> blobs = gcpStorage.list(
                storage.getBucketName(),
                Storage.BlobListOption.prefix(prefix)
        );

        List<String> blobNames = new ArrayList<>();
        for (Blob blob : blobs.iterateAll()) {
            blobNames.add(blob.getName());
        }
        return blobNames;
    }
}
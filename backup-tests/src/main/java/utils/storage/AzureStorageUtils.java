package utils.storage;

import api.abs.generated.model.DtoAzureStorage;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobClientBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.models.AccessTier;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.ListBlobsOptions;
import lombok.experimental.UtilityClass;
import utils.AerospikeLogger;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class AzureStorageUtils {
    public static AccessTier getAzureStorageClass(String blobPath, DtoAzureStorage storage) {
        // Authenticate using Client Secret
        ClientSecretCredential credential = new ClientSecretCredentialBuilder()
                .tenantId(storage.getTenantId())
                .clientId(storage.getClientId())
                .clientSecret(storage.getClientSecret())
                .build();

        // Create a Blob Client
        BlobClient blobClient = new BlobClientBuilder()
                .endpoint(storage.getEndpoint()) // Example: "https://mystorageaccount.blob.core.windows.net/"
                .containerName(storage.getContainerName())
                .blobName(blobPath)
                .credential(credential)
                .buildClient();

        // Fetch blob properties
        BlobProperties properties = blobClient.getProperties();
        return properties.getAccessTier(); // Returns "Hot", "Cool", "Cold" or "Archive"
    }

    public static List<String> listAzureBlobs(String path, DtoAzureStorage storage) {
        AerospikeLogger.info("List Azure objects in " + path);
        ClientSecretCredential credential = new ClientSecretCredentialBuilder()
                .tenantId(storage.getTenantId())
                .clientId(storage.getClientId())
                .clientSecret(storage.getClientSecret())
                .build();

        BlobContainerClient containerClient = new BlobContainerClientBuilder()
                .endpoint(storage.getEndpoint())
                .containerName(storage.getContainerName())
                .credential(credential)
                .buildClient();

        List<String> blobNames = new ArrayList<>();

        // List blobs under the given virtual folder (prefix)
        String prefix = path.endsWith("/") ? path : path + "/";
        for (BlobItem blobItem : containerClient.listBlobs(new ListBlobsOptions().setPrefix(prefix), null)) {
            blobNames.add(blobItem.getName());
        }

        return blobNames;
    }
}

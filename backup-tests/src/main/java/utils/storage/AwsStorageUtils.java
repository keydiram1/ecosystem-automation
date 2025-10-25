package utils.storage;

import api.abs.generated.model.DtoS3Storage;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;
import utils.AerospikeLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class AwsStorageUtils {

    @SneakyThrows
    public static StorageClass getS3StorageClass(String path, DtoS3Storage storage) {
        // Create an S3 client for MinIO
        HeadObjectResponse response;
        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(storage.getAccessKeyId(), storage.getSecretAccessKey()));
        try (S3Client s3Client = S3Client.builder()
                .credentialsProvider(credentialsProvider)
                .region(Region.of(storage.getS3Region()))
                .build()) {

            // Get object metadata
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(storage.getBucket())
                    .key(path)
                    .build();

            response = s3Client.headObject(request);
        }
        return response.storageClass();
    }

    @SneakyThrows
    public static List<String> listS3Objects(String path, DtoS3Storage storage) {
        AerospikeLogger.info("List s3 objects in " + path);
        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(storage.getAccessKeyId(), storage.getSecretAccessKey()));

        try (S3Client s3Client = S3Client.builder()
                .credentialsProvider(credentialsProvider)
                .region(Region.of(storage.getS3Region()))
                .build()) {

            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(storage.getBucket())
                    .prefix(path.endsWith("/") ? path : path + "/")
                    .build();

            // Paginate through results
            ListObjectsV2Iterable listResponses = s3Client.listObjectsV2Paginator(listRequest);

            return listResponses.stream()
                    .flatMap(page -> page.contents().stream())
                    .map(S3Object::key)
                    .toList();
        }
    }
}

package api.abs;

import api.abs.generated.ApiResponse;
import api.abs.generated.model.*;
import com.google.gson.reflect.TypeToken;
import lombok.experimental.UtilityClass;
import okhttp3.Call;
import org.awaitility.Awaitility;
import utils.AerospikeLogger;
import utils.ConfigParametersHandler;

import java.io.File;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static api.abs.API.restoreApi;
import static api.abs.generated.model.DtoJobStatus.JobStatusDone;
import static api.abs.generated.model.DtoJobStatus.JobStatusFailed;

@UtilityClass
public class AbsRestoreApi {

    private static final Duration DEFAULT_WAIT_TIMEOUT = Duration.ofMinutes(2);

    public static JobID restoreTimestamp(DtoRestoreTimestampRequest restoreRequest) {
        return new JobID(restoreApi.restoreTimestamp(restoreRequest));
    }

    public static DtoRestoreJobStatus restoreTimestampSync(DtoRestoreTimestampRequest restoreRequest) {
        return waitForRestore(restoreTimestamp(restoreRequest));
    }

    public static JobID restoreFull(DtoRestoreRequest restoreRequest) {
        return new JobID(restoreApi.restoreFull(restoreRequest));
    }

    public static JobID restoreFull(String path, String routineName) {
        return restoreFull(path, routineName, defaultPolicy(), null);
    }

    public static JobID restoreFull(String path, String routineName, DtoRestorePolicy policy) {
        return restoreFull(path, routineName, policy, null);
    }

    public static JobID restoreFull(String path, String routineName, DtoRestorePolicy policy, DtoSecretAgent secretAgent) {
        DtoRestoreRequest request = createRestoreRequest(path, routineName, policy, secretAgent);
        return new JobID(restoreApi.restoreFull(request));
    }

    public static DtoRestoreJobStatus restoreFullSync(DtoRestoreRequest restoreRequest) {
        return waitForRestore(new JobID(restoreApi.restoreFull(restoreRequest)));
    }

    public static DtoRestoreJobStatus restoreFullSync(String path, String routineName) {
        return restoreFullSync(path, routineName, defaultPolicy(), DEFAULT_WAIT_TIMEOUT);
    }

    public static DtoRestoreJobStatus restoreFullSync(String path, String routineName, Duration timeout) {
        return restoreFullSync(path, routineName, defaultPolicy(), timeout);
    }

    public static DtoRestoreJobStatus restoreFullSync(String path, String routineName, DtoRestorePolicy policy) {
        return restoreFullSync(path, routineName, policy, DEFAULT_WAIT_TIMEOUT);
    }

    public static DtoRestoreJobStatus restoreFullSync(String path, String routineName, DtoRestorePolicy policy, Duration timeout) {
        return waitForRestore(restoreFull(path, routineName, policy), timeout);
    }

    public static JobID restoreIncremental(DtoRestoreRequest restoreRequest) {
        return new JobID(restoreApi.restoreIncremental(restoreRequest));
    }

    public static JobID restoreIncremental(DtoStorage storage, DtoAerospikeCluster cluster) {
        return restoreIncremental(storage, cluster, defaultPolicy());
    }

    public static JobID restoreIncremental(DtoStorage storage, DtoAerospikeCluster cluster, DtoRestorePolicy policy) {
        return restoreIncremental(new DtoRestoreRequest()
                .policy(policy)
                .source(storage)
                .backupDataPath("/")
                .destination(cluster));
    }

    public static JobID restoreIncremental(String path, String routineName) {
        return restoreIncremental(path, routineName, defaultPolicy());
    }

    public static JobID restoreIncremental(String path, String routineName, DtoRestorePolicy policy) {
        DtoRestoreRequest request = createRestoreRequest(path, routineName, policy, null);
        return new JobID(restoreApi.restoreIncremental(request));
    }

    public static DtoRestoreJobStatus restoreIncrementalSync(String path, String routineName) {
        return restoreIncrementalSync(path, routineName, defaultPolicy(), DEFAULT_WAIT_TIMEOUT);
    }

    public static DtoRestoreJobStatus restoreIncrementalSync(String path, String routineName, Duration timeout) {
        return restoreIncrementalSync(path, routineName, defaultPolicy(), timeout);
    }

    public static DtoRestoreJobStatus restoreIncrementalSync(String path, String routineName, DtoRestorePolicy policy) {
        return restoreIncrementalSync(path, routineName, policy, DEFAULT_WAIT_TIMEOUT);
    }

    public static DtoRestoreJobStatus restoreIncrementalSync(String path, String routineName, DtoRestorePolicy policy, Duration timeout) {
        return waitForRestore(restoreIncremental(path, routineName, policy), timeout);
    }

    public static DtoRestoreJobStatus getRestoreStatus(JobID jobId) {
        return restoreApi.restoreStatus(jobId.value());
    }

    public static ApiResponse<String> cancelRestore(long jobId) {
        return restoreApi.cancelRestoreWithHttpInfo(jobId);
    }

    public static Map<String, DtoRestoreJobStatus> getJobs(Long from, Long to, String status) {
        return restoreApi.retrieveRestoreJobs(from, to, status);
    }

    // sends getStatus request with jobId = null.
    // should be used only in negative tests.
    public static String getRestoreStatusForNull() {
        Call call = restoreApi.restoreStatusCall(null, null);
        ApiResponse<String> response = restoreApi.getApiClient().execute(call, new TypeToken<String>() {
        }.getType());
        return response.getData();
    }

    public static DtoRestoreJobStatus waitForRestore(JobID jobId) {
        return waitForRestore(jobId, DEFAULT_WAIT_TIMEOUT);
    }

    public static DtoRestoreJobStatus waitForRestore(JobID jobId, Duration timeout) {
        return Awaitility.await()
                .pollInterval(1, TimeUnit.SECONDS)
                .atMost(timeout)
                .until(() -> {
                    var restoreStatus = AbsRestoreApi.getRestoreStatus(jobId);
                    if (restoreStatus.getStatus() == JobStatusFailed) {
                        throw new RuntimeException("Failed to restore");
                    }
                    return restoreStatus;
                }, restoreStatus -> restoreStatus.getStatus() == JobStatusDone);
    }

    public static void waitForRestoreFail(JobID jobId) {
        Awaitility.await()
                .pollInterval(2, TimeUnit.SECONDS)
                .atMost(DEFAULT_WAIT_TIMEOUT)
                .until(() -> {
                    var restoreStatus = AbsRestoreApi.getRestoreStatus(jobId);
                    if (restoreStatus.getStatus() == JobStatusDone) {
                        throw new RuntimeException("Expected to fail");
                    }
                    return restoreStatus.getStatus() == JobStatusFailed;
                });
    }

    public static DtoRestorePolicy defaultPolicy() {
        int restoreParallelismNum = Integer.parseInt(ConfigParametersHandler.getParameter("CONFIG_RESTORE_PARALLEL"));
        AerospikeLogger.info("The restore parallelism number was set to: " + restoreParallelismNum);
        return new DtoRestorePolicy()
                .noGeneration(true)
                .parallel(restoreParallelismNum);
    }

    public ApiResponse<File> retrieveAsConfFile(String name, Long timestamp) {
        return restoreApi.retrieveConfigurationWithHttpInfo(name, timestamp);
    }

    private static DtoRestoreRequest createRestoreRequest(String path, String routineName, DtoRestorePolicy policy, DtoSecretAgent secretAgent) {
        var routine = AbsRoutineApi.getRoutine(routineName);
        var storage = AbsStorageApi.getStorage(routine.getStorage());
        var destination = AbsClusterApi.getCluster(routine.getSourceCluster());
        return new DtoRestoreRequest()
                .backupDataPath(path)
                .source(storage)
                .destination(destination)
                .policy(policy)
                .secretAgent(secretAgent);
    }
}
package api.backup;

import api.RestUtils;
import api.backup.dto.BackgroundJob;
import com.google.common.base.Preconditions;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import lombok.experimental.UtilityClass;
import org.apache.http.HttpStatus;
import org.awaitility.Awaitility;
import utils.AutoUtils;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

import static api.backup.dto.BackgroundJob.BackgroundJobStatus.RUNNING;

@UtilityClass
public class JobAPI {

    public static void resumeJob(String jobId, long minutesToWait) {
        Response response = RestUtils.printRequest(RestAssured.given(BackupBaseRequests.getBaseRequestSpec("/v1/job/resume/" + jobId)).when()).post();
        Preconditions.checkState(response.getStatusCode() == HttpStatus.SC_OK);
        RestUtils.printResponse(response, "resumeJob");
        Callable<BackgroundJob> checkJobStatus = () -> getJob(jobId);
        Predicate<BackgroundJob> isDone = job -> job.getStatus() != RUNNING;
        Awaitility.waitAtMost(Duration.ofMinutes(minutesToWait)).pollInterval(Duration.ofSeconds(1)).until(checkJobStatus, isDone);
    }

    public static Response cancelJob(String jobId) {
        Response response = RestUtils.printRequest(RestAssured.given(BackupBaseRequests.getBaseRequestSpec("/v1/job/cancel/" + jobId)).when()).post();
        Preconditions.checkState(response.getStatusCode() == HttpStatus.SC_ACCEPTED);
        RestUtils.printResponse(response, "cancelJob");
        AutoUtils.sleep(2000);
        return response;
    }

    public static BackgroundJob getJob(String jobId) {
        for (int retries = 0; retries < 3; retries++) {
            Response responseJob = RestUtils.printRequest(RestAssured.given(BackupBaseRequests.getBaseRequestSpec("/v1/job/" + jobId)))
                    .get();
            RestUtils.printResponse(responseJob, "restore job with restore id " + jobId);
            try {
                return responseJob.body().as(BackgroundJob.class);
            } catch (Exception e) {
                e.printStackTrace();
                AutoUtils.sleep(500);
            }
        }
        return null;
    }

}

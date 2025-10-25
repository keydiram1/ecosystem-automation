package utils;

import api.RestUtils;
import api.abs.AbsApi;
import api.backup.BackupBaseRequests;
import com.aerospike.client.AerospikeException;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.InvocationBuilder;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.awaitility.Awaitility;
import utils.aerospike.adr.AerospikeDataUtils;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;


@UtilityClass
public class DockerManager {
    private static final DockerClient dockerClient = DockerClientBuilder.getInstance().build();
    public static final String REST_BACKEND = "adr-rest-backend";

    public static void restartAerospikeContainer(String containerId) throws DockerException {
        AerospikeLogger.info("Restart container " + containerId);
        dockerClient.restartContainerCmd(containerId).exec();
        Awaitility.await().until(() -> isContainerRunning(containerId));

        InspectContainerResponse containerInfo = dockerClient.inspectContainerCmd(containerId).exec();
        Map<ExposedPort, Ports.Binding[]> bindings = containerInfo.getNetworkSettings().getPorts().getBindings();
        int sourcePort = bindings.keySet().stream()
                .filter(it -> bindings.get(it) != null)
                .mapToInt(ExposedPort::getPort)
                .min().getAsInt();
        AerospikeLogger.info("Wait container to start on port " + sourcePort);
        Awaitility.await("Ensure that %s node is up".formatted(containerId))
                .pollInterval(1, TimeUnit.SECONDS)
                .until(() -> {
                    try {
                        AerospikeDataUtils.createSourceClient(sourcePort).close();
                        return true;
                    } catch (AerospikeException ignored) {
                        return false;
                    }
                });
    }

    public static void startContainer(String containerId) throws DockerException {
        AerospikeLogger.info("Start container " + containerId);
        if (!isContainerRunning(containerId)) {
            dockerClient.startContainerCmd(containerId).exec();
        }
    }

    public static void stopContainer(String containerId) throws DockerException {
        AerospikeLogger.info("Stop container " + containerId);
        if (isContainerRunning(containerId)) {
            dockerClient.stopContainerCmd(containerId).exec();
        }
    }

    public static String getHostname(String container) {
        return dockerClient.inspectContainerCmd(container).exec()
                .getNetworkSettings().getNetworks().values().iterator().next().getIpAddress();
    }

    public static boolean containerExist(String containerName) {
        return dockerClient.listContainersCmd().exec().stream()
                .map(container -> container.getNames()[0].substring(1)) // Remove leading '/'
                .anyMatch(containerName::equals);
    }

    @SneakyThrows
    public static boolean isContainerRunning(String containerId) throws DockerException {
        String status = dockerClient.inspectContainerCmd(containerId).exec().getState().getStatus();
        return "running".equals(status);
    }

    // return memory in megabytes
    @SneakyThrows
    public long getMemoryUsage(String containerId) {
        InvocationBuilder.AsyncResultCallback<Statistics> callback = new InvocationBuilder.AsyncResultCallback<>();
        dockerClient.statsCmd(containerId).exec(callback);
        Statistics statistics = callback.awaitResult();
        callback.close();
        return statistics.getMemoryStats().getUsage() / 1048576L;
    }

    public static void startAndWaitForRestBackend() {
        startContainer(REST_BACKEND);
        for (int i = 0; i < 20; i++) {
            try {
                Response response = RestUtils.printRequest(RestAssured.given(BackupBaseRequests.getBaseRequestSpec("")).when()).get();
                RestUtils.printResponse(response, "healthcheck");
                if (response.body().asPrettyString().equals("ADR Rest Backend"))
                    break;
            } catch (Exception e) {
                AerospikeLogger.info("Waiting for Rest Backend to start");
            }
            AutoUtils.sleep(5000);
        }
    }

    public static String getLogFromContainer(String containerName, int tail) {
        String containerId = getContainerIdByName(containerName);
        LogContainerCmd logContainerCmd = dockerClient.logContainerCmd(containerId)
                .withStdOut(true)
                .withStdErr(false);
        if (tail >= 0)
            logContainerCmd.withTail(tail);

        return executeLogContainerCmd(logContainerCmd);
    }

    public static String getLogFromContainer(String containerName) {
        return getLogFromContainer(containerName, -1);
    }

    private static String executeLogContainerCmd(LogContainerCmd logContainerCmd) {
        try {
            StringJoiner logContent = new StringJoiner("\n");
            logContainerCmd.exec(new ResultCallback.Adapter<Frame>() {
                @Override
                public void onNext(Frame frame) {
                    try {
                        logContent.add(new String(frame.getPayload()));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).awaitCompletion();

            return logContent.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String getContainerIdByName(String containerName) {
        List<Container> containers = dockerClient.listContainersCmd().withShowAll(true).exec();
        return containers.stream()
                .filter(container -> container.getNames() != null && container.getNames().length > 0 && container.getNames()[0].contains(containerName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Container not found: " + containerName))
                .getId();
    }

    public static void startAndWaitForBackupService() {
        DockerManager.startContainer("backup-service");
        Awaitility.await()
                .pollInterval(1, TimeUnit.SECONDS)
                .atMost(10, TimeUnit.SECONDS)
                .ignoreExceptions()
                .untilAsserted(() -> {
                    assertThat(AbsApi.getApiDocs().getStatusCode()).isEqualTo(200);
                });
    }

    public static void restartAbsAerospikeContainer(String containerId) throws DockerException {
        AerospikeLogger.info("Restart container " + containerId);
        dockerClient.restartContainerCmd(containerId).exec();
        Awaitility.await().until(() -> isContainerRunning(containerId));

        InspectContainerResponse containerInfo = dockerClient.inspectContainerCmd(containerId).exec();
        Map<ExposedPort, Ports.Binding[]> bindings = containerInfo.getNetworkSettings().getPorts().getBindings();
        int sourcePort = bindings.keySet().stream()
                .filter(it -> bindings.get(it) != null)
                .mapToInt(ExposedPort::getPort)
                .min().getAsInt();
        AerospikeLogger.info("Wait container to start on port " + sourcePort);
        Awaitility.await("Ensure that %s node is up".formatted(containerId))
                .pollInterval(1, TimeUnit.SECONDS)
                .until(() -> {
                    try {
                        utils.aerospike.abs.AerospikeDataUtils.createSourceClient();
                        return true;
                    } catch (AerospikeException ignored) {
                        return false;
                    }
                });
        AutoUtils.sleep(2000);
    }
}

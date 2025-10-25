package utils;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.Config;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import utils.abs.AbsRunner;

@UtilityClass
public class K8sUtils {
    private static final String K8S_NAMESPACE = AbsRunner.ENV_WORKSPACE;
    private static final LRUCache<Long, Boolean> printedLogs = new LRUCache<>(512);

    public static void printAllK8sPodLogs() {
        printAllK8sPodLogs(true);
    }

    public static void printAllK8sPodLogs(boolean checkIfLogContainsWarningOrError) {
        try {
            AerospikeLogger.info("Creating K8S client");
            ApiClient client = Config.defaultClient();
            Configuration.setDefaultApiClient(client);

            // Retrieve all pods using the updated method
            CoreV1Api api = new CoreV1Api();
            V1PodList podList = api.listNamespacedPod(K8S_NAMESPACE).execute();

            // Iterate over each pod and print the logs
            for (V1Pod pod : podList.getItems()) {
                if (pod.getMetadata() != null && pod.getSpec() != null) {
                    String podName = pod.getMetadata().getName();
                    String containerName = pod.getSpec().getContainers().get(0).getName(); // Assuming one container per pod

                    // Retrieve the logs for the pod and container
                    String logs = api.readNamespacedPodLog(podName, K8S_NAMESPACE)
                            .container(containerName)
                            .execute();

                    if (!checkIfLogContainsWarningOrError) {
                        AerospikeLogger.info("Logs from pod " + podName + ":");
                        AerospikeLogger.infoToFile(StringUtils.right(logs, 7_000_000));
                        AerospikeLogger.info(StringUtils.right(logs, 7_000_000));
                        return;
                    }

                    String podLogToPrint = StringUtils.right(logs, 50000);
                    if (podLogToPrint.contains("ERROR") || podLogToPrint.contains("WARN")) {
                        int endIndex = Math.min(5000, podLogToPrint.length());
                        long logHash = podLogToPrint.substring(0, endIndex).hashCode();
                        if (!printedLogs.containsKey(logHash)) {
                            printedLogs.add(logHash);
                            AerospikeLogger.info("Logs from pod " + podName + ":");
                            AerospikeLogger.info(podLogToPrint);
                        }
                    }
                } else {
                    AerospikeLogger.error("K8s pod variables are null");
                }
            }
        } catch (Exception e) {
            AerospikeLogger.info("The method printAllK8sPodLogs failed with the following exception: " + ExceptionUtils.getStackTrace(e));
        }
    }

    public static void printPodsStatistics() {
        AutoUtils.runBashCommand("kubectl top pod -n aerospike");
    }
}
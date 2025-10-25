package utils.aerospike;

import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Info;
import com.aerospike.client.cluster.Node;
import com.aerospike.client.policy.InfoPolicy;
import com.aerospike.client.policy.QueryDuration;
import com.aerospike.client.policy.QueryPolicy;
import com.aerospike.client.query.RecordSet;
import com.aerospike.client.query.Statement;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.awaitility.Awaitility;
import utils.AerospikeLogger;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@UtilityClass
public class AerospikeCountUtils {
    private static final int POLICY_TIMEOUT = 8_000;

    public static int getNamespaceObjectCount(IAerospikeClient client, String namespace) {
        Node[] allNodes = client.getNodes();
        if (allNodes.length == 0) {
            AerospikeLogger.info("No nodes in cluster");
            return 0;
        }

        int totalSetObjectCount = Arrays.stream(allNodes)
                .mapToInt(node -> getRecordsCountForNamespace(node, namespace))
                .sum();

        AerospikeLogger.info("Total number of object in NS " + namespace + " in all the nodes: " + totalSetObjectCount);
        return totalSetObjectCount;
    }

    public static int getSetObjectCount(IAerospikeClient client, String setName, String namespace) {
        AerospikeLogger.info("Starting getSetObjectCount");
        Statement statement = new Statement();
        statement.setNamespace(namespace);
        statement.setSetName(setName);

        QueryPolicy shortPolicy = buildQueryPolicy(QueryDuration.SHORT);
        QueryPolicy longPolicy = buildQueryPolicy(QueryDuration.LONG_RELAX_AP);

        int count = 0;
        try {
            count = countRecords(client, shortPolicy, statement);
        } catch (Exception e) {
            AerospikeLogger.info("Short query failed, retrying with LONG_RELAX_AP: " + e.getMessage());
            try {
                count = countRecords(client, longPolicy, statement);
            } catch (Exception ex) {
                AerospikeLogger.info("Long query also failed: " + ex.getMessage());
                throw new RuntimeException("Failed to count records in set " + setName, ex);
            }
        }

        AerospikeLogger.info("Accurate record count in set %s (namespace %s): %d"
                .formatted(setName, namespace, count));
        return count;
    }

    private static QueryPolicy buildQueryPolicy(QueryDuration duration) {
        QueryPolicy policy = new QueryPolicy();
        policy.includeBinData = false;
        policy.expectedDuration = duration;
        policy.maxRetries = 5;
        policy.sleepBetweenRetries = 1000;
        policy.totalTimeout = 10000;
        policy.socketTimeout = 5000;
        return policy;
    }

    private static int countRecords(IAerospikeClient client, QueryPolicy policy, Statement statement) {
        int count = 0;
        try (RecordSet rs = client.query(policy, statement)) {
            while (rs.next()) {
                count++;
            }
        }
        return count;
    }


    public static List<String> getNodeAddresses(IAerospikeClient client) {
        List<String> nodeAddresses = Arrays.stream(client.getNodes())
                .map(node -> node.getHost().name + ":" + node.getHost().port)
                .toList();

        AerospikeLogger.info("Found %d node(s) with addresses: %s".formatted(nodeAddresses.size(), nodeAddresses));
        return nodeAddresses;
    }

    public static int getObjectCountForNode(IAerospikeClient client, String namespace, String ipAndPort) {
        Node[] allNodes = client.getNodes();
        if (allNodes.length == 0) {
            AerospikeLogger.info("No nodes in cluster");
            return 0;
        }

        AerospikeLogger.info("Getting logical (master) object count for namespace '%s' on node %s"
                .formatted(namespace, ipAndPort));

        return Arrays.stream(allNodes)
                .filter(node -> {
                    String nodeAddress = node.getHost().name + ":" + node.getHost().port;
                    return nodeAddress.equals(ipAndPort);
                })
                .findFirst()
                .map(node -> {
                    int rawCount = getRecordsCountForNamespace(node, namespace);

                    AerospikeLogger.info("Logical (master_objects) count on node %s: %d"
                            .formatted(ipAndPort, rawCount));

                    return rawCount;
                })
                .orElseGet(() -> {
                    AerospikeLogger.info("Node with address %s not found in the cluster.".formatted(ipAndPort));
                    return 0;
                });
    }

    public static int getSetObjectCountForNode(IAerospikeClient client, String setName, String namespace, String
            nodeIp) {
        Node[] allNodes = client.getNodes();
        if (allNodes.length == 0) {
            AerospikeLogger.info("No nodes in cluster");
            return 0;
        }

        int replicationFactor = replicationFactor(allNodes[0], namespace);
        int effectiveReplicationFactor = Math.min(allNodes.length, replicationFactor);

        AerospikeLogger.info("Getting object count for set '%s' in namespace '%s' on node %s (effective replication factor=%d)"
                .formatted(setName, namespace, nodeIp, effectiveReplicationFactor));

        return Arrays.stream(allNodes)
                .filter(node -> node.getHost().name.equals(nodeIp))
                .findFirst()
                .map(node -> {
                    int count = getCountInSet(node, setName, namespace);
                    AerospikeLogger.info("Raw object count for set '%s' on node %s: %d"
                            .formatted(setName, nodeIp, count));
                    int adjustedCount = count / effectiveReplicationFactor;
                    AerospikeLogger.info("Adjusted (divided by replication factor) object count for set '%s' on node %s: %d"
                            .formatted(setName, nodeIp, adjustedCount));
                    return adjustedCount;
                })
                .orElseGet(() -> {
                    AerospikeLogger.info("Node with IP %s not found in the cluster.".formatted(nodeIp));
                    return 0;
                });
    }


    public static int replicationFactor(Node node, String namespace) {
        String request = sendInfoRequest(node, "get-config:context=namespace;id=" + namespace);
        String factor = StringUtils.substringBetween(request, "replication-factor=", ";");
        if (factor == null) {
            return 1;
        }
        return Integer.parseInt(factor);
    }

    private static int getCountInSet(Node node, String setName, String namespace) {
        String nodeInfo = sendInfoRequest(node, "sets");
        String setObjectCountInNode = StringUtils.substringBetween(nodeInfo, "ns=" + namespace + ":set=" + setName + ":objects=", ":");
        if (setObjectCountInNode == null) {
            return 0;
        }
        return Integer.parseInt(setObjectCountInNode);
    }

    private static int getRecordsCountForNamespace(Node node, String namespace) {
        String nodeInfo = sendInfoRequest(node, "namespace/" + namespace);
        String setObjectCountInNode = StringUtils.substringBetween(nodeInfo, "master_objects=", ";");
        if (setObjectCountInNode == null) {
            return 0;
        }
        return Integer.parseInt(setObjectCountInNode);
    }

    private static int getCountInNS(Node node, String namespace) {
        String nodeInfo = sendInfoRequest(node, "sets");
        return Arrays.stream(nodeInfo.split(";")) // ; separates info on different namespaces
                .filter(it -> it.contains("ns=" + namespace + ":") || it.contains("ns=" + namespace + ";"))
                .flatMap(it -> Arrays.stream(it.split(":"))) // : separates properties of a namespace
                .filter(it -> it.startsWith("objects="))
                .map(it -> it.replace("objects=", ""))
                .mapToInt(Integer::parseInt)
                .sum();
    }

    public static void waitForNamespaceTruncate(IAerospikeClient client, String namespace) {
        Awaitility.await()
                .pollDelay(Duration.ofSeconds(1))
                .pollInterval(Duration.ofSeconds(10))
                .atMost(1, TimeUnit.MINUTES)
                .alias("Truncate namespace " + namespace)
                .until(() -> isNamespaceEmpty(client, namespace));
        AerospikeLogger.info("Namespace: " + namespace + " truncated.");
    }

    public static void waitForSetTruncate(IAerospikeClient client, String namespace, String set) {
        Awaitility.await()
                .pollDelay(Duration.ofMillis(500))
                .pollInterval(Duration.ofSeconds(3))
                .atMost(30, TimeUnit.SECONDS)
                .alias("Truncate namespace " + namespace + " set " + set)
                .until(() -> isSetEmpty(client, namespace, set));
        AerospikeLogger.info("Namespace: " + namespace + ", set: " + set + " truncated.");
    }

    private static boolean isNamespaceEmpty(IAerospikeClient client, String namespace) {
        QueryPolicy queryPolicy = new QueryPolicy();
        queryPolicy.includeBinData = false;
        queryPolicy.shortQuery = true;
        Statement statement = new Statement();
        statement.setNamespace(namespace);
        statement.setMaxRecords(1);
        RecordSet rs = client.query(queryPolicy, statement);
        return !rs.iterator().hasNext();
    }

    public static boolean isSetEmpty(IAerospikeClient client, String namespace, String set) {
        QueryPolicy queryPolicy = new QueryPolicy();
        queryPolicy.includeBinData = false;
        queryPolicy.shortQuery = true;
        Statement statement = new Statement();
        statement.setNamespace(namespace);
        statement.setSetName(set);
        statement.setMaxRecords(1);
        RecordSet rs = client.query(queryPolicy, statement);
        return !rs.iterator().hasNext();
    }

    private static String sendInfoRequest(Node node, String name) {
        InfoPolicy infoPolicy = new InfoPolicy();
        infoPolicy.setTimeout(POLICY_TIMEOUT);
        String request = Info.request(null, node, name);
        AerospikeLogger.info("Info request " + name + ":" + request);

        return request;
    }
}
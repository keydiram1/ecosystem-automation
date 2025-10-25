package utils;

import lombok.experimental.UtilityClass;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.Instance;
import utils.abs.AbsRunner;

@UtilityClass
public class AwsUtils {

    public static final String AWS_WORKSPACE = ConfigParametersHandler.getParameter("aws_env_workspace");

    private static final Ec2Client ec2Client = Ec2Client.builder()
            .region(Region.EU_CENTRAL_1)
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build();

    public static String getEc2PrivateIp(String instanceId) {
        return getEc2Instance(instanceId).privateIpAddress();
    }

    public static String getEc2PublicIp(String instanceId) {
        return getEc2Instance(instanceId).publicIpAddress();
    }

    @SuppressWarnings("UnusedReturnValue")
    public static String runBashCommandOnSourceCluster(String command) {
        return runBashCommandOnCluster(command, ClusterName.SOURCE, 1, true);
    }

    @SuppressWarnings("UnusedReturnValue")
    public static String runBashCommandOnCluster(String command, ClusterName clusterName, int nodeNumber, boolean printLog) {
        return AutoUtils.runBashCommand("aerolab attach shell -n " + AWS_WORKSPACE + "-" + clusterName + "-cluster" + " -l " + nodeNumber + " -- " + command, printLog);
    }

    public static synchronized String runGcCloudCommandOnCluster(String command) {
        AutoUtils.sleep(1000); // sleep one seconds between every ssh command

        String gcloudCommand = "gcloud compute ssh --zone \"me-west1-a\" \"ubuntu@" + AbsRunner.ENV_WORKSPACE + "-asdb-node-0\"" +
                " --project \"ecosystem-connectors-data\" --quiet --command \"" + command + "\"";

        return AutoUtils.runBashCommand(new String[]{"bash", "-c", gcloudCommand});
    }

    public static Instance getEc2Instance(String instanceId) {
        DescribeInstancesRequest describeInstancesRequest = DescribeInstancesRequest.builder()
                .instanceIds(instanceId)
                .build();
        return ec2Client.describeInstances(describeInstancesRequest)
                .reservations().get(0)
                .instances().get(0);
    }

    public enum ClusterName {
        SOURCE("source"),
        BACKUP("backup");
        private final String data;

        ClusterName(final String data) {
            this.data = data;
        }

        @Override
        public String toString() {
            return data;
        }
    }
}

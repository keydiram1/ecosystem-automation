package utils;

import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Info;
import lombok.experimental.UtilityClass;

import java.lang.module.ModuleDescriptor;

@UtilityClass
public class VersionValidator {

    public static boolean hasRequiredAerospikeVersion(IAerospikeClient client, String required) {
        String actualVersion = getServerVersion(client);
        return ModuleDescriptor.Version.parse(actualVersion).compareTo(
                ModuleDescriptor.Version.parse(required)) >= 0;
    }

    private static String getServerVersion(IAerospikeClient client) {
        String versionString = Info.request(client.getCluster().getRandomNode(), "version");
        return versionString.substring(versionString.lastIndexOf(' ') + 1);
    }
}

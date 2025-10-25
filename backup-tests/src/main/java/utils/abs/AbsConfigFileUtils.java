package utils.abs;

import utils.AutoUtils;

import java.nio.file.Files;
import java.nio.file.Paths;

public class AbsConfigFileUtils {

    public static void addRoutine(String routineName, String namespace) {
        String gitRoot = AutoUtils.runBashCommand(new String[]{
                "/bin/sh", "-c", "git rev-parse --show-toplevel"
        }, false, false).split("\\R")[0].trim();

        String configFile = Paths.get(gitRoot, "devops/install/abs/conf/service/config.yml").toString();
        if (!Files.exists(Paths.get(configFile))) return;

        String block = String.join("\\n",
                "    " + routineName + ":",
                "        interval-cron: \\\"@yearly\\\"",
                "        source-cluster: absDefaultCluster",
                "        storage: local",
                "        namespaces: [\\\"" + namespace + "\\\"]",
                "        backup-policy: defaultPolicy"
        );

        String cmd = "awk '/^backup-routines:/ { print; print \"" + block + "\"; next }1' \"" +
                configFile + "\" > \"" + configFile + ".tmp\" && mv \"" + configFile + ".tmp\" \"" + configFile + "\"";

        AutoUtils.runBashCommand(new String[]{"/bin/sh", "-c", cmd});
    }

    public static void editService(int size, int tps) {
        String gitRoot = AutoUtils.runBashCommand(new String[]{
                "/bin/sh", "-c", "git rev-parse --show-toplevel"
        }, false, false).split("\\R")[0].trim();

        String configFile = Paths.get(gitRoot, "devops/install/abs/conf/service/config.yml").toString();
        if (!Files.exists(Paths.get(configFile))) return;

        String block = String.join("\n",
                "  http:",
                "    rate:",
                "      size: " + size,
                "      tps: " + tps
        );

        String cmd = "printf '%s\\n' \"" + block + "\" >> \"" + configFile + "\"";

        AutoUtils.runBashCommand(new String[]{"/bin/sh", "-c", cmd});
    }

}
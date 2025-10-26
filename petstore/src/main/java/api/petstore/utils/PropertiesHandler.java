package api.petstore.utils;

import lombok.experimental.UtilityClass;

import java.io.FileInputStream;
import java.util.Properties;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class PropertiesHandler {

    public static final String QA_ENV_PROPERTY = "qa_environment";

    public static void setProperties() {
        if (getParameter(QA_ENV_PROPERTY) == null) {
            System.setProperty(QA_ENV_PROPERTY, "LOCAL");
        }
        addSystemProperties("build" + getParameter(QA_ENV_PROPERTY) + ".properties");
        addSystemProperties("../devops/install/backup/.env");
    }

    public static void addSystemProperties(String pathToPropertiesFile) {
        Properties prp = new Properties();
        try (FileInputStream inputStream = new FileInputStream(pathToPropertiesFile)) {
            prp.load(inputStream);
        } catch (Exception ignored) {
            // ignore
        }
        for (Object key : prp.keySet()) {
            if (getParameter((String) key) == null)
                System.setProperty((String) key, prp.getProperty((String) key));
        }
    }

    @NonNls
    public static String getParameter(@NonNls @NotNull String key) {

        String retVal = System.getProperty(key, null);
        if (retVal == null) {
            return System.getenv(key);
        }
        return retVal;
    }
}

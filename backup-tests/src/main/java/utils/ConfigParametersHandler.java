package utils;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class ConfigParametersHandler {
    @NonNls
    public static String getParameter(@NonNls @NotNull String key) {

        String retVal = System.getProperty(key, null);
        if (retVal == null) {
            return System.getenv(key);
        }
        return retVal;
    }
}

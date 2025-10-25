package utils.files;

import lombok.experimental.UtilityClass;

import java.io.FileInputStream;
import java.util.Properties;
import utils.ConfigParametersHandler;

@UtilityClass
public class PropertiesHandler {

	public static final String QA_ENV_PROPERTY = "qa_environment";

	public static void setProperties() {
		if (ConfigParametersHandler.getParameter(QA_ENV_PROPERTY) == null) {
			System.setProperty(QA_ENV_PROPERTY, "LOCAL");
		}
		addSystemProperties("build" + ConfigParametersHandler.getParameter(QA_ENV_PROPERTY) + ".properties");
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
			if (ConfigParametersHandler.getParameter((String) key) == null)
				System.setProperty((String) key, prp.getProperty((String) key));
		}
	}
}

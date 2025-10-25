package api.abs;

import api.abs.generated.ApiResponse;
import api.abs.generated.model.DtoConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.nio.file.Files;
import java.nio.file.Paths;

import static api.abs.API.configurationApi;

@UtilityClass
public class AbsConfigApi {

    ObjectMapper mapper = new ObjectMapper();

    public static DtoConfig getConfiguration() {
        return configurationApi.readConfig();
    }

    @SneakyThrows
    public static ApiResponse<Void> updateConfigurationWithFile(String filePath) {
        String fileContent = new String(Files.readAllBytes(Paths.get(filePath)));
        DtoConfig configuration = mapper.readValue(fileContent, DtoConfig.class);
        return updateConfiguration(configuration);
    }

    public static ApiResponse<Void> updateConfiguration(DtoConfig configuration) {
        return configurationApi.updateConfigWithHttpInfo(configuration);
    }

    public static void apply() {
        configurationApi.applyConfig();
    }
}

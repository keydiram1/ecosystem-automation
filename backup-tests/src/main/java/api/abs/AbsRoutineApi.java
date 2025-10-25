package api.abs;

import api.abs.generated.ApiResponse;
import api.abs.generated.model.DtoBackupRoutine;
import lombok.experimental.UtilityClass;

import java.util.Map;

import static api.abs.API.configurationApi;

@UtilityClass
public class AbsRoutineApi {

    public static Map<String, DtoBackupRoutine> getAllRoutines() {
        return configurationApi.readRoutines();
    }

    public static DtoBackupRoutine getRoutine(String name) {
        return configurationApi.readRoutine(name);
    }

    public static ApiResponse<Void> createRoutine(String name, DtoBackupRoutine routine) {
        return configurationApi.addRoutineWithHttpInfo(name, routine);
    }

    public static ApiResponse<Void> updateRoutine(String name, DtoBackupRoutine routine) {
        return configurationApi.updateRoutineWithHttpInfo(name, routine);
    }

    public static ApiResponse<Void> deleteRoutine(String name) {
        return configurationApi.deleteRoutineWithHttpInfo(name);
    }

    public static String getAnyNamespaceForRoutine(DtoBackupRoutine routine) {
        if (routine.getNamespaces() == null || routine.getNamespaces().isEmpty()) {
            return "source-ns1";
        }
        return routine.getNamespaces().get(0);
    }

    public static String getAnyNamespaceForRoutine(String routineName) {
        return getAnyNamespaceForRoutine(getRoutine(routineName));
    }

    public static void disable(String name) {
        configurationApi.disableRoutine(name);
    }

    public static void enable(String name) {
        configurationApi.enableRoutine(name);
    }
}

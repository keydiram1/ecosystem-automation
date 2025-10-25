package api.backup.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BackgroundJob {
    private Long id;
    private BackgroundJobType type;
    private Long processed;
    private Long failed;
    private Long created;
    private Long updated;
    private BackgroundJobStatus status;
    private List<String> failedKeys;
    private byte[] partitions;
    private int percentDone;

    public enum BackgroundJobType {
        RESTORE,
        DELETE
    }

    public enum BackgroundJobStatus {
        INIT,
        RUNNING,
        QUALIFYING,
        DONE,
        CANCELED,
        FAILED
    }
}

package api.backup.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestoreRecordsRequest {
    private String srcClusterName;
    private String trgClusterName;
    private String srcNS;
    private String trgNS;
    private String set;
    private Long fromTime;
    private Long toTime;
    private String[] srcDigests;
    private String[] bins;
}

package api.backup.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestoreSetRequest {
    private String srcClusterName;
    private String trgClusterName;
    private String srcNS;
    private String trgNS;
    private String set;
    private Number fromTime;
    private Number toTime;
    private String[] bins;
    private BackgroundJobPolicy backgroundJobPolicy;
}

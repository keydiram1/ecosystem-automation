package api.backup.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SMDPolicy {
    private long duration; //in seconds
    private long keepFor; //in seconds, 0 to keep forever
}

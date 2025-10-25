package api.backup.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackgroundJobPolicy {

    @Min(1)
    @Max(1000) // S3 batch limitation
    private Integer recordsBatchSize;

    private Integer partitionFilter;

    private Integer parallelism;
}

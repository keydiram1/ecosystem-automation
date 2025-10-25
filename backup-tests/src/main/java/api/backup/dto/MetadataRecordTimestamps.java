package api.backup.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetadataRecordTimestamps {

    private String srcDigest;

    private String set;

    private Collection<Long> timestamps;
}

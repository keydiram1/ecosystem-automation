package api.backup.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RetrieveEntityRecord {
    private String srcDigest;
    private Map<String, Object> bins;
    private long timestamp;
    private boolean delete;
}
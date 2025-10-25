package api.backup.dto;

import lombok.Data;

@Data
public class ContinuousBackup {

	private String name;
	private String description;
	private String srcClusterName;
	private String srcNS;
	private String backupNS;
	private String policy;
    private long lastExecuted;
	private String status;
    private long lastCompaction; // epoch milliseconds

	public ContinuousBackup(String name, String description, String srcClusterName, String srcNS, String backupNS, String policy) {
		this.name = name;
		this.description = description;
		this.srcClusterName = srcClusterName;
		this.srcNS = srcNS;
		this.backupNS = backupNS;
		this.policy = policy;
	}
}

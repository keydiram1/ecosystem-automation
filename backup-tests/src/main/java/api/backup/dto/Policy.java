package api.backup.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Policy {
	private final String name;
	private final int duration;
	private final int retention;
	private final int keepFor;
	private final int maxThroughput;
	private final Integer initialSync;

	public Policy(String name, int duration) {
        this(name, duration, 0, 0,100_000, 0);
	}
}

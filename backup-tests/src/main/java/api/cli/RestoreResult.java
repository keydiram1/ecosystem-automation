package api.cli;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Data
@AllArgsConstructor
public class RestoreResult {
    private ZonedDateTime startTime;
    private String duration;
    private int recordsRead;
    private int sIndexRead;
    private int udfsRead;
    private int expiredRecords;
    private int skippedRecords;
    private int ignoredRecords;
    private int fresherRecords;
    private int existedRecords;
    private int insertedRecords;
    private long totalBytesRead;
    private int exitCode;

    public static RestoreResult getInstance(String commandOutput, boolean ignoreErrors) {
        if (!ignoreErrors && commandOutput.contains("ERROR")) {
            throw new RestoreProcessException("An error occurred during the restore process: " + commandOutput);
        }
        String startTime = extractValue(commandOutput, "Start Time:");
        String duration = extractValue(commandOutput, "Duration:");
        int recordsRead = Integer.parseInt(extractValue(commandOutput, "Records Read:"));
        int sIndexRead = Integer.parseInt(extractValue(commandOutput, "sIndex Read:"));
        int udfsRead = Integer.parseInt(extractValue(commandOutput, "UDFs Read:"));
        int expiredRecords = Integer.parseInt(extractValue(commandOutput, "Expired Records:"));
        int skippedRecords = Integer.parseInt(extractValue(commandOutput, "Skipped Records:"));
        int ignoredRecords = Integer.parseInt(extractValue(commandOutput, "Ignored Records:"));
        int fresherRecords = Integer.parseInt(extractValue(commandOutput, "Fresher Records:"));
        int existedRecords = Integer.parseInt(extractValue(commandOutput, "Existed Records:"));
        int insertedRecords = Integer.parseInt(extractValue(commandOutput, "Inserted Records:"));
        long totalBytesRead = 0;
        String value = extractValue(commandOutput, "Total Bytes Read:");
        if (value != null && !value.isEmpty()) { // In xdr restore "Total Bytes Read" doesn't exist
            totalBytesRead = Long.parseLong(value);
        }
        int exitCode = Integer.parseInt(extractValue(commandOutput, "Exit Code:"));

        ZonedDateTime zonedDateTime = ZonedDateTime.parse(startTime, DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss z", Locale.ENGLISH));

        return new RestoreResult(zonedDateTime, duration, recordsRead, sIndexRead, udfsRead, expiredRecords, skippedRecords, ignoredRecords, fresherRecords, existedRecords, insertedRecords, totalBytesRead, exitCode);
    }

    public static RestoreResult getInstance(String commandOutput) {
        return getInstance(commandOutput, false);
    }

    @Override
    public String toString() {
        return "RestoreReport {" +
                "Start Time='" + startTime + '\'' +
                ", Duration='" + duration + '\'' +
                ", Records Read=" + recordsRead +
                ", sIndex Read=" + sIndexRead +
                ", UDFs Read=" + udfsRead +
                ", Expired Records=" + expiredRecords +
                ", Skipped Records=" + skippedRecords +
                ", Ignored Records=" + ignoredRecords +
                ", Fresher Records=" + fresherRecords +
                ", Existed Records=" + existedRecords +
                ", Inserted Records=" + insertedRecords +
                ", Total Bytes Read=" + totalBytesRead +
                ", Exit Code=" + exitCode +
                '}';
    }

    public static String extractValue(String output, String key) {
        String[] lines = output.split("\n");
        for (String line : lines) {
            if (line.startsWith(key)) {
                int colonIndex = line.indexOf(':');
                if (colonIndex != -1 && colonIndex + 1 < line.length()) {
                    return line.substring(colonIndex + 1).trim(); // Trim leading spaces
                }
            }
        }
        return "";
    }

    public double getDurationMillis() {
        if (duration == null) return 0;
        String trimmed = duration.trim();
        if (trimmed.endsWith("ms")) {
            return Double.parseDouble(trimmed.replace("ms", ""));
        } else if (trimmed.endsWith("s")) {
            return Double.parseDouble(trimmed.replace("s", "")) * 1000;
        }
        throw new IllegalArgumentException("Unknown duration format: " + duration);
    }
}
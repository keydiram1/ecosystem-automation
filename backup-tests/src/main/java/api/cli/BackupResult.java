package api.cli;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Data
@AllArgsConstructor
public class BackupResult {
    private long startTime;
    private String duration;
    private int recordsRead;
    private int sIndexRead;
    private int udfsRead;
    private long bytesWritten;
    private int filesWritten;
    private int exitCode;
    private String backupDir;
    private String fullLog;

    public static BackupResult getInstance(String commandOutput, boolean ignoreErrors) {
        if (!ignoreErrors && commandOutput.contains("ERROR")) {
            throw new BackupProcessException("An error occurred during the backup process: " + commandOutput);
        }
        String startTime = extractValue(commandOutput, "Start Time:");
        String duration = extractValue(commandOutput, "Duration:");
        int recordsRead;
        String recordsReadStr = extractValue(commandOutput, "Records Read:");
        if (recordsReadStr.isEmpty()) {
            recordsReadStr = extractValue(commandOutput, "Records Received:");
        }
        if (recordsReadStr.isEmpty()) {
            throw new BackupProcessException("Neither 'Records Read' nor 'Records Received' were found in the command output.");
        }
        recordsRead = Integer.parseInt(recordsReadStr);
        int sIndexRead = Integer.parseInt(extractValue(commandOutput, "sIndex Read:"));
        int udfsRead = Integer.parseInt(extractValue(commandOutput, "UDFs Read:"));
        long bytesWritten = Long.parseLong(extractValue(commandOutput, "Bytes Written:").replace(" bytes", ""));
        int filesWritten = Integer.parseInt(extractValue(commandOutput, "Files Written:"));
        int exitCode = Integer.parseInt(extractValue(commandOutput, "Exit Code:"));

        long startDateTime = ZonedDateTime.parse(startTime, DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss z", Locale.ENGLISH)
        ).withZoneSameLocal(ZoneId.of("Asia/Jerusalem")).toInstant().toEpochMilli();
        return new BackupResult(startDateTime, duration, recordsRead, sIndexRead, udfsRead, bytesWritten, filesWritten, exitCode, "", commandOutput);
    }

    public static BackupResult getInstance(String commandOutput) {
        return getInstance(commandOutput, false);
    }

    @Override
    public String toString() {
        return "BackupReport {" +
                "Start Time='" + startTime + '\'' +
                ", Duration='" + duration + '\'' +
                ", Records Read=" + recordsRead +
                ", sIndex Read=" + sIndexRead +
                ", UDFs Read=" + udfsRead +
                ", Bytes Written=" + bytesWritten +
                ", Files Written=" + filesWritten +
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
        String rawDuration = extractValue(fullLog, "Duration:");
        if (rawDuration.endsWith("ms")) {
            return Double.parseDouble(rawDuration.replace("ms", ""));
        } else if (rawDuration.endsWith("s")) {
            return Double.parseDouble(rawDuration.replace("s", "")) * 1000;
        } else if (rawDuration.endsWith("m")) {
            return Double.parseDouble(rawDuration.replace("m", "")) * 60_000;
        } else {
            // Fallback: assume it's in seconds if no unit
            return Double.parseDouble(duration) * 1000;
        }
    }
}

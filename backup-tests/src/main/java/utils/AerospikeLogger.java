package utils;

import com.epam.reportportal.message.TypeAwareByteSource;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.utils.files.Utils;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;

@UtilityClass
public class AerospikeLogger {

    private static final List<Object[]> SECRETS_LIST = new ArrayList<>();

    public static <T> void info(T varToPrint) {
        info(varToPrint, null);
    }

    public static <T> void info(T varToPrint, boolean shouldMaskSecrets) {
        info(varToPrint, null, shouldMaskSecrets);
    }

    public static <T> void info(T varToPrint, File file) {
        info(varToPrint, file, false);
    }

    public static <T> void info(T varToPrint, File file, boolean shouldMaskSecrets) {
        String textToPrint = varToPrint.toString();
        if (shouldMaskSecrets) {
            textToPrint = maskText(varToPrint);
        }
        if (file == null) {
            List<String> stringsToPrintList = splitStringIntoParts(textToPrint, 500_000);
            getLogger().ifPresent(logger -> {
                for (String stringToPrint : stringsToPrintList) {
                    System.out.println(stringToPrint);
                    sendLogToReportPortal("INFO", stringToPrint, null);
                }
            });
        } else {
            sendLogToReportPortal("INFO", "File Attached", file);
        }
    }

    public static <T> void error(T varToPrint) {
        error(varToPrint, null);
    }

    public static <T> void error(T varToPrint, Throwable t) {
        getLogger().ifPresent(logger -> {
            logger.error(varToPrint.toString(), t);
            sendLogToReportPortal("ERROR", varToPrint.toString(), null);
        });
    }

    public static <T> void infoToFile(T varToPrint) {
        try {
            File tempFile = File.createTempFile("log", ".log");
            try (FileWriter writer = new FileWriter(tempFile)) {
                writer.write(varToPrint.toString());
            }
            info(varToPrint, tempFile);
            tempFile.deleteOnExit();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void logFileByPath(String absoluteFilePath) {
        File file = new File(absoluteFilePath);
        if (!file.exists() || !file.isFile()) {
            error("File does not exist or is not a valid file: " + absoluteFilePath);
            return;
        }

        try {
            sendLogToReportPortal("INFO", "File Attached: " + absoluteFilePath, file);
        } catch (Exception e) {
            error("Failed to log file to ReportPortal: " + absoluteFilePath, e);
        }
    }

    private static synchronized void sendLogToReportPortal(String level, String log, @Nullable File file) {
        ReportPortal.emitLog(itemUuid -> {
            SaveLogRQ rq = new SaveLogRQ();
            rq.setItemUuid(itemUuid);
            rq.setLevel(level);
            rq.setLogTime(Calendar.getInstance().getTime());
            rq.setMessage(log);
            rq.setLogTime(Calendar.getInstance().getTime());
            if (file != null) {
                try {
                    rq.setFile(createFileDto(file));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return rq;
        });
    }

    private static Optional<Logger> getLogger() {
        try {
            Class<?> clazz = Class.forName(getCallerClass());
            return Optional.ofNullable(LoggerFactory.getLogger(clazz));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    private static String getCallerClass() {
        String clazz = Thread.currentThread().getStackTrace()[4].toString();
        clazz = StringUtils.substringBeforeLast(clazz, "(");
        return StringUtils.substringBeforeLast(clazz, ".");
    }

    private static SaveLogRQ.File createFileDto(File file) throws IOException {
        TypeAwareByteSource data = Utils.getFile(file);
        SaveLogRQ.File fileDto = new SaveLogRQ.File();
        fileDto.setContent(data.read());
        fileDto.setContentType(data.getMediaType());
        fileDto.setName(file.getName());
        return fileDto;
    }

    private static List<String> splitStringIntoParts(String longString, int maxPartLength) {
        List<String> splitStrings = new ArrayList<>();
        int startIndex = 0;
        while (startIndex < longString.length()) {
            int endIndex = Math.min(startIndex + maxPartLength, longString.length());
            String part = longString.substring(startIndex, endIndex);
            splitStrings.add(part);
            startIndex = endIndex;
        }
        return splitStrings;
    }

    static {
        SECRETS_LIST.add(new Object[]{"client_id", 20});
        SECRETS_LIST.add(new Object[]{"client-id", 20});
        SECRETS_LIST.add(new Object[]{"client-secret", 20});
        SECRETS_LIST.add(new Object[]{"secret-access-key", 20});
        SECRETS_LIST.add(new Object[]{"access-key-id", 20});
        SECRETS_LIST.add(new Object[]{"BEGIN PRIVATE KEY", 1000});
        SECRETS_LIST.add(new Object[]{"client-id", 20});
        SECRETS_LIST.add(new Object[]{"\"endpoint\":\"", 20});
        SECRETS_LIST.add(new Object[]{"tenant-id\":\"", 20});
        SECRETS_LIST.add(new Object[]{"private_key_id", 30});
        SECRETS_LIST.add(new Object[]{"s3-secret-access-key", 30});
        SECRETS_LIST.add(new Object[]{"s3-access-key-id", 20});
    }

    private static <T> String maskText(T varToPrint) {
        String text = varToPrint.toString();
        StringBuilder maskedText = new StringBuilder(text);
        for (Object[] rule : SECRETS_LIST) {
            String maskStart = (String) rule[0];
            int maskLength = (Integer) rule[1];
            int startIndex = maskedText.indexOf(maskStart);
            while (startIndex != -1) {
                int maskStartIndex = startIndex + maskStart.length();
                int endIndex = Math.min(maskStartIndex + maskLength, maskedText.length());

                boolean toggle = true;
                for (int i = maskStartIndex; i < endIndex; i++) {
                    maskedText.setCharAt(i, toggle ? '!' : '#');
                    toggle = !toggle;
                }

                startIndex = maskedText.indexOf(maskStart, maskStartIndex + maskLength);
            }
        }
        return maskedText.toString();
    }
}

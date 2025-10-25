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

    public static <T> void info(T varToPrint) {
        info(varToPrint, null);
    }

    public static <T> void info(T varToPrint, File file) {
        if (file == null) {
            List<String> stringsToPrintList = splitStringIntoParts(varToPrint.toString(), 500_000);
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

    public static <T> void warn(T varToPrint) {
        warn(varToPrint, null);
    }

    public static <T> void warn(T varToPrint, Throwable t) {
        getLogger().ifPresent(logger -> {
            logger.warn(varToPrint.toString(), t);
            sendLogToReportPortal("WARN", varToPrint.toString(), null);
        });
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

    private static void sendLogToReportPortal(String level, String log, @Nullable File file) {
        ReportPortal.emitLog(itemUuid -> {
            SaveLogRQ rq = new SaveLogRQ();
            rq.setItemUuid(itemUuid);
            rq.setLevel(level);
            rq.setLogTime(Calendar.getInstance().getTime());
            rq.setMessage(log);
            rq.setLogTime(Calendar.getInstance().getTime());
            if (file != null) {
                try {
                    rq.setFile(createFileModel(file));
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

    private static SaveLogRQ.File createFileModel(File file) throws IOException {
        TypeAwareByteSource data = Utils.getFile(file);
        SaveLogRQ.File fileModel = new SaveLogRQ.File();
        fileModel.setContent(data.read());
        fileModel.setContentType(data.getMediaType());
        fileModel.setName(file.getName());
        return fileModel;
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
}

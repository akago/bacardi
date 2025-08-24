package se.kth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.models.MavenErrorLog;

import java.io.File;
import java.io.IOException;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws IOException {
        Map<String, String> options = parseArgs(args);

        if (options.containsKey("help") || !options.containsKey("log") {
            printUsage();
            return;
        }

        File logFile = new File(options.get("log"));
        if (!logFile.exists()) {
            log.error("Log file not found: {}", logFile.getAbsolutePath());
            return;
        }
        log.info("Analyzing log file: {}", logFile.getAbsolutePath());

        FailureCategoryExtract failureCategoryExtract = new FailureCategoryExtract(logFile);


        MavenErrorInformation mavenErrorInformation = new MavenErrorInformation(logFile);
        try {
            MavenErrorLog errorLog = mavenErrorInformation.extractLineNumbersWithPaths(String.valueOf(logFile));
            System.out.println(errorLog);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println(failureCategoryExtract.getFailureCategory(logFile));
    }

}
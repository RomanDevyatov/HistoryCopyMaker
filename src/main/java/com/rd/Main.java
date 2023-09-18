package com.rd;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.logging.Logger;

public class Main {

    private static final Logger logger = Logger.getLogger(HistoryCopyMaker.class.getName());

    public static void main(String... args) {
        try {
            if (args.length == 0) {
                logger.info("No args provided. Please add the path to the prepared folder.");
                System.exit(0);
            }

            logger.info("PROGRAM PARAMETERS, Args: " + Arrays.asList(args));

            String generalFolderPath = args[0];
            String browserType = getBrowserType(args);
            boolean isLogFile = isLogFileEnabled(args);
            String pathToBrowserHistoryOverwritten = getPathToBrowserHistoryOverwritten(args);

            logger.info("Program (main function) has been started at: " + (new GregorianCalendar()).toZonedDateTime());

            HistoryCopyMaker historyCopyMaker = new HistoryCopyMaker(
                    generalFolderPath,
                    browserType,
                    isLogFile,
                    pathToBrowserHistoryOverwritten
            );
            historyCopyMaker.startProcess();

            logger.info("Program (main function) has been finished");
        } catch (Exception e) {
            logger.info("Error: " + e.getMessage());
            System.exit(-1);
        }
    }

    private static String getBrowserType(String[] args) {
        String browserType = HistoryCopyMaker.CHROME_BROWSER_TYPE;
        if (args.length > 1) {
            browserType = args[1].toLowerCase();
        }
        return browserType;
    }

    private static boolean isLogFileEnabled(String[] args) {
        return args.length > 2 && args[2].equals("log_on");
    }

    private static String getPathToBrowserHistoryOverwritten(String[] args) {
        String pathToBrowserHistory = StringUtils.EMPTY;
        if (args.length > 3) {
            pathToBrowserHistory = args[3];
        }
        return pathToBrowserHistory;
    }

}

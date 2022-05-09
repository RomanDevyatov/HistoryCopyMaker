package com.rd;

import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.logging.Logger;

public class Main {

    private static final Logger logger = Logger.getLogger(HistoryCopyMaker.class.getName());

    public static void main(String... args) {
        try {
            if (args.length == 0) {
                logger.info("no args, add path to prepared folder");
                System.exit(0);
            }

            logger.info("PROGRAM PARAMETERS, Args: " + Arrays.asList(args));

            String generalFolderPath = args[0];

            String browserType = HistoryCopyMaker.CHROME_BROWSER_TYPE;
            if (args.length > 1) {
                browserType = args[1].toLowerCase();
            }

            boolean isLogFile = false;
            if (args.length > 2 && args[2].equals("log_on")) {
                isLogFile = true;
            }

            logger.info("Program (main function) has been started at: " + (new GregorianCalendar()).toZonedDateTime());

            HistoryCopyMaker historyCopyMaker = new HistoryCopyMaker(generalFolderPath, browserType, isLogFile);
            historyCopyMaker.startProcess();

            logger.info("Program (main function) has been finished");
        } catch (Exception e) {
            logger.info("Error: " + e.getMessage());
            System.exit(-1);
        }
    }
}

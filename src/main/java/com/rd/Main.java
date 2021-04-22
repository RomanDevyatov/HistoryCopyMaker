package com.rd;

import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.logging.Logger;

public class Main {

    private static final Logger log = Logger.getLogger(ChromeHistoryCopyMaker.class.getName());

    public static void main(String... args) {
        try {
            log.info("Program has been started at: " + (new GregorianCalendar()).toZonedDateTime());
            String generalFolderPath = args[0];
            log.info("Args: " + Arrays.asList(args));
            ChromeHistoryCopyMaker chromeHistoryVisitAnalyzer = new ChromeHistoryCopyMaker(generalFolderPath);
            chromeHistoryVisitAnalyzer.startProcess();
            log.info("Program has been finished");
        } catch (Exception e) {
            log.info("Error: " + e.getMessage());
            System.exit(-1);
        }
    }
}

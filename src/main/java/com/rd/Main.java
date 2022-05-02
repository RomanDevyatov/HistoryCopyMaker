package com.rd;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Main {

    private static final Logger logger = Logger.getLogger(HistoryCopyMaker.class.getName());
    private static Handler fileHandler = null;

    public static void setup(String generalFolderPath) {
        try {
            String pathToLogFile = Paths.get(generalFolderPath, "log", HistoryCopyMaker.USER_NAME + "_copyMaker.log").normalize().toString();
            File logFile = new File(pathToLogFile);
            logFile.getParentFile().mkdirs();
            logFile.createNewFile();
            logger.info("Log file is created: " + pathToLogFile);

            fileHandler = new FileHandler(pathToLogFile);
            SimpleFormatter simple = new SimpleFormatter();
            fileHandler.setFormatter(simple);

            logger.addHandler(fileHandler);

        } catch (IOException e) {
            logger.severe("Error in setup function: " + e.getMessage());
        }
    }

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

            if (args.length > 2 && args[2].equals("log_on")) {
                setup(generalFolderPath);
            }

            logger.info("Program (main function) has been started at: " + (new GregorianCalendar()).toZonedDateTime());

            HistoryCopyMaker historyCopyMaker = new HistoryCopyMaker(generalFolderPath, browserType);
            historyCopyMaker.startProcess();

            logger.info("Program (main function) has been finished");
        } catch (Exception e) {
            logger.info("Error: " + e.getMessage());
            System.exit(-1);
        }
    }
}

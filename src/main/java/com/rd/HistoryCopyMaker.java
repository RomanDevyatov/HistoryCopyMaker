package com.rd;

import com.rd.comparators.RecordComparator;
import com.rd.models.HistoryRecord;
import com.rd.utils.FileUtility;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.sqlite.SQLiteConfig;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.GregorianCalendar;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class HistoryCopyMaker extends FileUtility {

    private static final Logger logger = Logger.getLogger(HistoryCopyMaker.class.getName());

    private static Handler fileHandler = null;

    private Connection connection;
    private Statement statement;
    private ResultSet resultSet;
    private final String generalFolderFullPath;
    private final String browserType;
    private boolean isLogFile;
    private String dbHistoryPath;
    private String dbHistoryCopyPath;
    // TODO: move following variables into separated class
    public static final String CHROME_BROWSER_TYPE = "chrome";
    public static final String FIREFOX_BROWSER_TYPE = "firefox";

    public static final String HOME_PATH = System.getProperty("user.home");
    public static final String USER_NAME = System.getProperty("user.name");

    private static final String CHROME_DB_PATH_POSTFIX = "\\AppData\\Local\\Google\\Chrome\\User Data\\Default\\History";
    private static final String CHROME_DB_COPY_PATH_POSTFIX = "\\AppData\\Local\\Google\\Chrome\\User Data\\Default\\HistoryCopy";
    private static final String CHROME_DB_COPY_FILE_NAME = "HistoryCopy";
    private static final String FIREFOX_PROFILES_PATH_POSTFIX = "\\AppData\\Roaming\\Mozilla\\Firefox\\Profiles\\";
    private static final String FIREFOX_DB_FILE_NAME = "places.sqlite";
    private static final String FIREFOX_DB_COPY_FILE_NAME = "placesCopy.sqlite";
    // TODO: set FIREFOX_FOLDER_MASK as input parameter
    private static final String FIREFOX_FOLDER_MASK = "*.default-esr";

    private static final int sleepDurationMillis = 1000 * 20; // 20 seconds (ms)
    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


    public HistoryCopyMaker(String path, String browserType, boolean isLogFile, String pathToBrowserHistoryOverWritten) {
        this.generalFolderFullPath = path;
        this.browserType = browserType;
        this.isLogFile = isLogFile;

        defineDbPathString(browserType, pathToBrowserHistoryOverWritten);
    }

    public void startProcess() {
        Thread run = new Thread(() -> {
             while (true) {
                try {
                    Thread.sleep(sleepDurationMillis);
                } catch (Exception e) {
                    logger.severe("Error in thread: " + e.getMessage());
                } finally {
                    long startTime = System.nanoTime();

                    if (this.isLogFile) {
                        addFileHandler(this.generalFolderFullPath);
                    }

                    try {
                        createResFile(this.generalFolderFullPath, USER_NAME);
                        String dateStartString = getStartDate();
                        logger.info("Start date: " + dateStartString);
                        createCopyOfDBHistoryFile();
                        try {
                            resultSet = getQueryResultSetByDateFrom(dateStartString, this.dbHistoryCopyPath);

                            String pathToResultHistoryFile = Paths.get(this.generalFolderFullPath, "ResultHistory", fileName)
                                    .normalize()
                                    .toString();

                            Set<HistoryRecord> newHistoryRecordsSet = getNewHistoryRecordsSet(resultSet, pathToResultHistoryFile);

                            List<HistoryRecord> newHistoryRecordsSortedList = getSortedList(newHistoryRecordsSet);

                            String newRecordsString = newHistoryRecordsSortedList.stream()
                                    .map(HistoryRecord::toString)
                                    .collect(Collectors.joining("\n", "", "\n"));

                            writeToHistFile(newRecordsString, pathToResultHistoryFile);
                        } catch (SQLException exception) {
                            logger.severe("Error while executing SQL query: " + exception.getMessage());
                        } catch (ClassNotFoundException e) {
                            logger.severe("Error setting jdbc driver: " + e.getMessage());
                        } finally {
                            try {
                                resultSet.close();
                                statement.close();
                                connection.close();
                                logger.info("Connection to db is closed, OK");
                                if (!removeFile(this.dbHistoryCopyPath)) {
                                    logger.info("historyFileCopy is not deleted");
                                } else {
                                    logger.info("historyFileCopy is deleted");
                                }
                            } catch (Exception ex) {
                                logger.severe("Error in finally block: " + ex.getMessage());
                            }
                        }
                    } catch(ParseException e) {
                        logger.severe("Error while parsing date:" + e.getMessage());
                    } catch(IOException e) {
                        logger.severe("Error while creating historyFileCopy: " + e.getMessage());
                    }

                    long durationNanoSeconds   = System.nanoTime() - startTime;
                    long durationMilliSeconds = durationNanoSeconds / (long) Math.pow(10, 6);

                    logger.info("Clearing file handlers...");
                    logger.info("Execution time is " + durationMilliSeconds + " ms (1 sec = 1 000 ms)");
                    clearFileHandlers();
                }
            }
        });

        run.start();
    }

    private static void addFileHandler(String generalFolderPath) {
        try {
            String pathToLogFile = Paths.get(generalFolderPath, "log", USER_NAME + "_copyMaker_" + LocalDate.now() + ".log").normalize().toString();

            File logFile = new File(pathToLogFile);
            if (!logFile.exists()) {
                logFile.getParentFile().mkdirs();
                logFile.createNewFile();
                logger.info("Log file doesn't exist. New log file is created: " + pathToLogFile);
            } else {
                logger.info("Log file already exists: " + pathToLogFile);
            }

            fileHandler = new FileHandler(pathToLogFile, true);

            SimpleFormatter simple = new SimpleFormatter();
            fileHandler.setFormatter(simple);

            logger.addHandler(fileHandler);
        } catch (IOException e) {
            logger.severe("Error while adding handler: " + e.getMessage());
        }
    }

    private static void clearFileHandlers() {
        if (fileHandler != null) {
            fileHandler.close();
        }
        for (Handler handler : logger.getHandlers()) {
            logger.removeHandler(handler);
        }
    }

    private String getStartDate() throws ParseException {
        long timeLast = getTimeInMillisOfToday();

        return format.format(timeLast);
    }

    private long getTimeInMillisOfToday() {
        LocalDate date = LocalDate.now();
        Calendar calendarStart = new GregorianCalendar(date.getYear(), date.getMonth().getValue() - 1, date.getDayOfMonth(), 0, 0, 0);
        return calendarStart.getTimeInMillis();
    }

    private void createCopyOfDBHistoryFile() throws IOException {
        File historyFile = new File(this.dbHistoryPath);
        File historyFileCopy = new File(this.dbHistoryCopyPath);
        String historyFileCopyAbsolutePath = historyFileCopy.getAbsolutePath();

        if (historyFileCopy.createNewFile()) {
            logger.info("Copy of db history file is created: " + historyFileCopyAbsolutePath);
        } else {
            logger.info("Copy of db history file is not created: " + historyFileCopyAbsolutePath);
        }

        copyFile(historyFile, historyFileCopy);
    }

    private List<HistoryRecord> getSortedList(Set<HistoryRecord> historyRecordSet) {
        return historyRecordSet.stream()
                .sorted(new RecordComparator())
                .collect(Collectors.toList());
    }

    private ResultSet getQueryResultSetByDateFrom(String dateFromString, String dbHistoryPath) throws ClassNotFoundException, SQLException {
        logger.info("Date From: " + dateFromString);
        Class.forName("org.sqlite.JDBC");

        SQLiteConfig config = new SQLiteConfig();
        config.setReadOnly(true);
        String dbPath = "jdbc:sqlite:" + dbHistoryPath;

        connection = DriverManager.getConnection(dbPath, config.toProperties());
        logger.info("Connection is established");

        statement = connection.createStatement();

        String query = getSqlRequest(dateFromString);

        return statement.executeQuery(query);
    }

    private Set<HistoryRecord> getNewHistoryRecordsSet(ResultSet resultSet, String pathToResultHistoryFile) throws SQLException, IOException {
        Set<HistoryRecord> urlsFromFile = getHistoryRecordSetFromHistoryFile(pathToResultHistoryFile);
        Set<HistoryRecord> urlsFromDB = convertResultToHistoryRecordSet(resultSet);

        Set<HistoryRecord> differenceSet = new HashSet<>(urlsFromDB);
        differenceSet.removeAll(urlsFromFile);

        return differenceSet;
    }

    private Set<HistoryRecord> getHistoryRecordSetFromHistoryFile(String fileName) throws IOException {
        Set<String> historyFileLinesSet;
        try (Stream<String> lines = Files.lines(Paths.get(fileName))) {
            historyFileLinesSet = lines.collect(Collectors.toSet());
        }

        Set<HistoryRecord> historyRecordSet = new HashSet<>();
        for (String s : historyFileLinesSet) {
            String[] args = s.split(HistoryRecord.HISTORY_RECORD_DELEMITER, 2);
            historyRecordSet.add(new HistoryRecord(args[0], args[1]));
        }

        return historyRecordSet;
    }

    private Set<HistoryRecord> convertResultToHistoryRecordSet(ResultSet resultSet) throws SQLException {
        Set<HistoryRecord> result = new HashSet<>();

        if (resultSet != null) {
            logger.info("Converting result lines to set. Processing...");
            while (resultSet.next()) {
                String url = resultSet.getString("url");
                String dateTime = resultSet.getString("local_last_visit_time");

                result.add(new HistoryRecord(url, dateTime));
            }
            logger.info("Converting result lines to set. DONE");
        } else {
            logger.info("ResultSet is null");
        }

        return result;
    }

    private void writeToHistFile(String output, String path) {
        try {
            if (!StringUtils.isBlank(output)) {
                logger.info("Adding these lines to Result History file (" + path + "): " + "\n" + output);
                Files.write(Paths.get(path), output.getBytes(), StandardOpenOption.APPEND);
                logger.info("Writing in file, OK");
            } else {
                logger.info("output is blank, OK");
            }
        } catch (IOException e) {
            logger.severe("Error in writing: " + e.getMessage());
        }
    }

    private String getSqlRequest(String dateFrom) {
        String queryString = "";

        switch (browserType) {
            case CHROME_BROWSER_TYPE:
                queryString = "SELECT url, datetime(last_visit_time / 1000000 + (strftime('%s', '1601-01-01')), 'unixepoch', 'localtime') as local_last_visit_time" + '\n'
                        + "FROM urls" + '\n'
                        + "WHERE " + " local_last_visit_time > " + "'" + dateFrom + "'" + '\n'
                        + "AND url LIKE '%hh.ru%'" +  ";";
                break;
            case FIREFOX_BROWSER_TYPE:
                queryString = "SELECT url, datetime(last_visit_date / 1000000, 'unixepoch','localtime') as local_last_visit_time" + '\n'
                        + "FROM moz_places" + '\n'
                        + "WHERE local_last_visit_time > " + "'" + dateFrom + "'" + '\n'
                        + "AND url LIKE '%hh.ru%'" + '\n'
                        + "AND visit_count > 0" + ";";
                break;
        }

        return queryString;
    }

    private void defineDbPathString(String browserType, String pathToBrowserHistoryOverWritten) {
        String dbPathString = "";
        String dbCopyPathString = "";

        if (StringUtils.isBlank(pathToBrowserHistoryOverWritten)) {
            logger.info("Taking default value for history path");
            if (browserType.equals(FIREFOX_BROWSER_TYPE)) {
                String fullPathToFirefoxProfilesString = Paths.get(HOME_PATH, FIREFOX_PROFILES_PATH_POSTFIX).normalize().toString();
                File rootProfilesFirefoxDirectory = new File(fullPathToFirefoxProfilesString);
                String[] filesNames = rootProfilesFirefoxDirectory.list(new WildcardFileFilter(FIREFOX_FOLDER_MASK));

                String firefoxDBFolder;
                if (filesNames != null && filesNames.length > 0) {
                    logger.info("Firefox folder was found by mask: " + FIREFOX_FOLDER_MASK);
                    firefoxDBFolder = filesNames[0];
                } else {
                    logger.severe(fullPathToFirefoxProfilesString + " doesn't contain folder by mask");
                    throw new RuntimeException("No firefox folder was found by mask!");
                }

                dbPathString = Paths.get(HOME_PATH, FIREFOX_PROFILES_PATH_POSTFIX, firefoxDBFolder, FIREFOX_DB_FILE_NAME).normalize().toString();
                dbCopyPathString = Paths.get(HOME_PATH, FIREFOX_PROFILES_PATH_POSTFIX, firefoxDBFolder, FIREFOX_DB_COPY_FILE_NAME).normalize().toString();
            } else if (this.browserType.equals(CHROME_BROWSER_TYPE)) {
                dbPathString = Paths.get(HOME_PATH, CHROME_DB_PATH_POSTFIX).normalize().toString();
                dbCopyPathString = Paths.get(HOME_PATH, CHROME_DB_COPY_PATH_POSTFIX).normalize().toString();
            }
        } else {
            logger.info("Taking overwritten value for history path");
            dbPathString = pathToBrowserHistoryOverWritten;
            dbCopyPathString = getCopyDbPathString(pathToBrowserHistoryOverWritten);
        }
        logger.info("Defined paths: dbPathString = " + dbPathString + ", dbCopyPathString = " + dbCopyPathString);

        this.dbHistoryPath = dbPathString;
        this.dbHistoryCopyPath = dbCopyPathString;
    }

    private String getCopyDbPathString(String pathToBrowserHistoryOverWritten) {
        int lastBackSlasheIndex = StringUtils.lastIndexOf(pathToBrowserHistoryOverWritten, "\\");

        String dbCopyPathString = "";
        if (lastBackSlasheIndex != -1) {
            String pathBeforeLastSlash = StringUtils.substring(pathToBrowserHistoryOverWritten, 0, lastBackSlasheIndex + 1);

            if (browserType.equals(FIREFOX_BROWSER_TYPE)) {
                dbCopyPathString = pathBeforeLastSlash + FIREFOX_DB_COPY_FILE_NAME;
            } else if (browserType.equals(CHROME_BROWSER_TYPE)) {
                dbCopyPathString = pathBeforeLastSlash + CHROME_DB_COPY_FILE_NAME;
            }
        } else {
            throw new RuntimeException("pathToBrowserHistoryOverWritten doesn't contain double back slash symbol ('\\')!");
        }

        return dbCopyPathString;
    }

}

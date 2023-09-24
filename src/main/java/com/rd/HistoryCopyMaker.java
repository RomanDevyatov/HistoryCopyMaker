package com.rd;

import com.rd.comparators.RecordComparator;
import com.rd.models.HistoryRecord;
import com.rd.utils.FileUtility;
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

import static com.rd.utils.Constants.*;


public class HistoryCopyMaker extends FileUtility {

    private static final Logger logger = Logger.getLogger(HistoryCopyMaker.class.getName());

    private static Handler fileHandler = null;

    private Connection connection;
    private Statement statement;
    private ResultSet resultSet;
    private final String workingDirectoryPath;
    private final String browserType;
    private final boolean isLogFile;
    private final String username;
    private final String dbHistoryPath;
    private final String dbHistoryCopyPath;

    private static final int sleepDurationMillis = 1000 * 15; // 15 seconds (ms)
    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


    public HistoryCopyMaker(String workingDirectoryPath,
                            String browserType,
                            boolean isLogFile,
                            String username,
                            String dbPathString,
                            String dbCopyPathString) {
        this.workingDirectoryPath = workingDirectoryPath;
        this.browserType = browserType;
        this.isLogFile = isLogFile;
        this.username = username;
        this.dbHistoryPath = dbPathString;
        this.dbHistoryCopyPath = dbCopyPathString;
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
                        addFileHandler(this.workingDirectoryPath, this.username);
                    }

                    try {
                        createResFile(this.workingDirectoryPath, this.username);
                        String dateStartString = getStartDate();
                        logger.info("Start date: " + dateStartString);
                        createCopyOfDBHistoryFile(this.dbHistoryPath, this.dbHistoryCopyPath);

                        try {
                            resultSet = getQueryResultSetByDateFrom(dateStartString, this.dbHistoryCopyPath, this.browserType);

                            String pathToResultHistoryFile = Paths.get(this.workingDirectoryPath, "ResultHistory", fileName)
                                    .normalize()
                                    .toString();

                            Set<HistoryRecord> newHistoryRecordsSet = getNewHistoryRecordsSet(resultSet, pathToResultHistoryFile);
                            List<HistoryRecord> newHistoryRecordsSortedList = getSortedList(newHistoryRecordsSet);

                            String newRecordsString = newHistoryRecordsSortedList.stream()
                                    .map(HistoryRecord::toString)
                                    .collect(Collectors.joining("\n", "", "\n"));

                            writeToHistoryFile(newRecordsString, pathToResultHistoryFile);
                        } catch (SQLException exception) {
                            logger.severe("Error while executing SQL query: " + exception.getMessage());
                        } catch (ClassNotFoundException e) {
                            logger.severe("Error setting jdbc driver: " + e.getMessage());
                        } finally {
                            closeDBResources();
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

    private void closeDBResources() {
        try {
            resultSet.close();
            statement.close();
            connection.close();
            logger.info("Connection to db is closed, OK");

            if (!removeFile(this.dbHistoryCopyPath)) {
                logger.info("Copy db history was not removed: " + this.dbHistoryCopyPath);
            } else {
                logger.info("Copy db history was removed:" + this.dbHistoryCopyPath);
            }
        } catch (Exception e) {
            logger.severe("Error in finally block: " + e.getMessage());
        }
    }

    private void addFileHandler(String generalFolderPath, String username) {
        try {
            String pathToLogFile = Paths.get(generalFolderPath, "log", username + "_copyMaker_" + LocalDate.now() + ".log").normalize().toString();

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
            e.printStackTrace();
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

    private void createCopyOfDBHistoryFile(String dbPath, String dbCopyPath) throws IOException {
        File historyFile = new File(dbPath);
        File historyFileCopy = new File(dbCopyPath);
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

    private ResultSet getQueryResultSetByDateFrom(String dateFromString, String dbHistoryPath, String browserType) throws ClassNotFoundException, SQLException {
        logger.info("Date From: " + dateFromString);
        Class.forName("org.sqlite.JDBC");

        SQLiteConfig config = new SQLiteConfig();
        config.setReadOnly(true);
        String dbPath = JDBC_SQLITE + dbHistoryPath;

        connection = DriverManager.getConnection(dbPath, config.toProperties());
        logger.info("Connection is established, OK");

        statement = connection.createStatement();

        String query = getSqlRequest(dateFromString, browserType);

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
        logger.info("Reading history file, fileName=" + fileName);
        try (Stream<String> lines = Files.lines(Paths.get(fileName))) {
            return lines.map(line -> line.split(HistoryRecord.HISTORY_RECORD_DELEMITER, 2))
                    .map(parts -> new HistoryRecord(parts[0], parts[1]))
                    .collect(Collectors.toSet());
        }
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
            logger.info("ResultSet is null, OK");
        }

        return result;
    }

    private void writeToHistoryFile(String output, String path) {
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

    private String getSqlRequest(String dateFrom, String browserType) {
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
}

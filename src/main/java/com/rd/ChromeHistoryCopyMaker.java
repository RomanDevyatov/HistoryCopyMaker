package com.rd;

import org.apache.commons.lang3.StringUtils;
import org.sqlite.SQLiteConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class ChromeHistoryCopyMaker extends FileUtility {

    private Connection connection;
    private Statement statement;
    private ResultSet resultSet;
    private final String generalFolderFullPath;

    private static final Logger log = Logger.getLogger(ChromeHistoryCopyMaker.class.getName());

    private static final String HOME_PATH = System.getProperty("user.home");
    private static final String USER_NAME = System.getProperty("user.name");
    private static final String HISTORY_PATH = "\\AppData\\Local\\Google\\Chrome\\User Data\\Default\\History";
    private static final String HISTORY_COPY_PATH = "\\AppData\\Local\\Google\\Chrome\\User Data\\Default\\History1";
    private static final int sleepDuration = 1000 * 60 * 1; // 1 minute(ms)
    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final String HH_RU_BASE_URL = "https://hh.ru";
    private static int tailMs = 1000 * 10; // ten seconds

    public ChromeHistoryCopyMaker(String path) {
        log.setLevel(Level.INFO);
        this.generalFolderFullPath = path;
    }

    public void startProcess() {
//        final AtomicLong startTimeMil = getMilTimeFromMorningOfToday();
        Thread run = new Thread(() -> {
             while (true) {
                try {
                    Thread.sleep(sleepDuration);
                } catch (Exception e) {
                    log.severe("Error in thread: " + e.getMessage());
                } finally {
                    try {
                        createResFile(this.generalFolderFullPath, USER_NAME);
                        long timeLast = getLastTimeFromResHist();
                        if (timeLast == 0) { // if file is empty then take current time
                            timeLast = ((new GregorianCalendar()).getTimeInMillis()) - sleepDuration - tailMs;
                        }
                        String dateStartString = format.format(timeLast);
//                    String dateFromString = format.format(startTimeMil.get());
//                    long nowTimeMil = ((new GregorianCalendar()).getTimeInMillis());
//                    startTimeMil.set(nowTimeMil);
                        File historyFile = new File(HOME_PATH + HISTORY_PATH);
                        File historyFileCopy = new File(HOME_PATH + HISTORY_COPY_PATH);
                        historyFileCopy.createNewFile();
                        copyFile(historyFile, historyFileCopy);
                        try {
                            resultSet = getQueryResultSetByDateFrom(dateStartString);
                            String outputUrlsResult = getHistRecordsForWriting(resultSet);
                            String pathToRes = this.generalFolderFullPath + "/ResultHistory/" + fileName;
                            appendToHistFile(outputUrlsResult, pathToRes);
                        } catch (SQLException exception) {
                            log.severe("Error while executing SQL query: " + exception.getMessage());
                        } catch (ClassNotFoundException e) {
                            log.severe("Error setting jdbc driver: " + e.getMessage());
                        } finally {
                            try {
                                resultSet.close();
                                statement.close();
                                connection.close();
                                if (!historyFileCopy.delete()) {
                                    log.info("historyFileCopy is not deleted");
                                } else {
                                    log.info("historyFileCopy is deleted");
                                }
                            } catch (Exception ex) {
                                log.severe("Error in finally block: " + ex.getMessage());
                            }
                        }
                    } catch(ParseException e) {
                        log.severe("Error while parsing date:" + e.getMessage());
                    } catch(IOException e) {
                        log.severe("Error while creating historyFileCopy: " + e.getMessage());
                    }
                }
            }
        });
        run.start();
    }

    private long getLastTimeFromResHist() throws ParseException {
        String lastRecord = getLastRecordOfResHist();
        if (lastRecord.equals("")) {
            return Long.parseLong("0");
        }
        String dateStr = StringUtils.substringAfterLast(lastRecord, "Visited On ");
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(dateStr).getTime();
    }

    private String getLastRecordOfResHist() {
        String lastRecord = "";
        try {
            String pathToResHist = this.generalFolderFullPath + "/ResultHistory/" + fileName;
            BufferedReader br = new BufferedReader(new FileReader(pathToResHist));
            try {
                String line;
                while ((line = br.readLine()) != null) {
                    lastRecord = line;
                }
            } finally {
                br.close();
            }
        } catch (IOException e) {
            log.severe("Error while finding last record");
        }

        return lastRecord;
    }

    private String getHistRecordsForWriting(ResultSet resultSet) {
        StringBuilder sb = new StringBuilder();
        try {
            if (resultSet != null) {
                while (resultSet.next()) {
                    String url = resultSet.getString("url");
                    if (filterUrl(url)) {
                        String line = url + ", Visited On " + resultSet.getString("local_last_visit_time");
                        log.info("line from history: " + line);
                        if (!sb.toString().contains(url + ", Visited On")) {
                            if (!isDuplicateInResultFile(url)) {
                                sb.append(line).append("\n");
                            } else {
                                log.info("Duplicate is found in Result File: " + line);
                            }
                        } else {
                            log.info("This line is already exists in resultSet, just pass it");
                        }
                    } else {
                        log.info("This url is incorrect, just pass it");
                    }
                }
            } else {
                log.info("ResultSet is null");
            }
        } catch (SQLException e) {
            log.severe("Error while executing next: " + e.getMessage());
        }

        return sb.toString();
    }

    private void appendToHistFile(String output, String path) {
        try {
            if (!StringUtils.isBlank(output)) {
                Files.write(Paths.get(path), output.getBytes(), StandardOpenOption.APPEND);
                log.info("Writing in file, OK");
            } else {
                log.info("outputUrlsResult is blank, OK");
            }
        } catch (IOException e) {
            log.severe("Error in writing: " + e.getMessage());
        }
    }

    private boolean isHhruUrl(String line) {
        return StringUtils.contains(line, HH_RU_BASE_URL);
    }

    private boolean filterUrl(String urlString) {
        if (urlString.contains(" ")) {
            log.info("This url is incorrect: url has one whitespace or more");
            return false;
        }
        if (!isHhruUrl(urlString)) {
            log.info("This url is not of hhru: " + urlString);
            return false;
        }
        return true;
    }

    private boolean isDuplicateInResultFile(String url) {
        String pathToResHist = this.generalFolderFullPath + "/ResultHistory/" + fileName;
        boolean found = false;
        try {
            BufferedReader br = new BufferedReader(new FileReader(pathToResHist));
            try {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.contains(url + ", Visited On")) {
                        found = true;
                        break;
                    }
                }
            } finally {
                br.close();
            }
        } catch(IOException ioe) {
            found = true;
            System.out.println("Error while opening the file: " + pathToResHist);
        }
        return found;
    }
//
//    private boolean isDuplicateInResultFileOld(String url) {
//        try (Stream<String> stream = Files.lines(Paths.get(this.generalFolderFullPath, "ResultHistory", fileName))) {
//            Optional<String> findString = stream
//                    .filter(str -> StringUtils.contains(str, url + ", Visited On"))
//                    .findFirst();
//            return findString.isPresent();
//        } catch (IOException e) {
//            log.severe("Error while reading the file for duplicate search: " + e.getMessage());
//        }
//        return true;
//    }

    private AtomicLong getMilTimeFromMorningOfToday() {
        LocalDate date = LocalDate.now();
        Calendar calendarStart = new GregorianCalendar(date.getYear(), date.getMonth().getValue() - 1, date.getDayOfMonth(), 0, 0, 0);
        return new AtomicLong(calendarStart.getTimeInMillis());
    }

    private ResultSet getQueryResultSetByDateFrom(String dateFromString) throws ClassNotFoundException, SQLException {
        log.info("Date From: " + dateFromString);
        Class.forName("org.sqlite.JDBC");
        SQLiteConfig config = new SQLiteConfig();
        config.setReadOnly(true);
        connection = DriverManager.getConnection("jdbc:sqlite:" + HOME_PATH + HISTORY_COPY_PATH, config.toProperties());
        log.info("Connection successfully established");

        statement = connection.createStatement();
        return statement.executeQuery(getSqlRequest(dateFromString));
    }

    private String getSqlRequest(String dateFrom) {
        return "SELECT URL, datetime(last_visit_time / 1000000 + (strftime('%s', '1601-01-01')), 'unixepoch', 'localtime') as local_last_visit_time" + '\n'
                + "FROM urls" + '\n'
                + "WHERE " + " local_last_visit_time > " + "'" + dateFrom + "'";
    }

}

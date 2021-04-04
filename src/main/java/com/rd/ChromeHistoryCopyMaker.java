package com.rd;

import org.sqlite.SQLiteConfig;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private static final int duration = 1000 * 60 * 20; // 20 minutes
    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public ChromeHistoryCopyMaker(String path) {
        log.setLevel(Level.INFO);
        this.generalFolderFullPath = path;
        createResFile(this.generalFolderFullPath, USER_NAME);
    }

    public void startProcess() {
        AtomicLong startTimeMil = getMilTimeFromMorningOfToday();
        Thread run = new Thread(() -> {
            while(true){
                try {
                    String dateFromString = format.format(startTimeMil.get());
                    long nowTimeMil = ((new GregorianCalendar()).getTimeInMillis());
                    startTimeMil.set(nowTimeMil);
                    Path historyPath = Paths.get(HOME_PATH, HISTORY_PATH),
                            historyCopyPath = Paths.get(HOME_PATH, HISTORY_COPY_PATH);
                    File historyFile = new File(String.valueOf(historyPath)),
                            historyFileCopy = new File(String.valueOf(historyCopyPath));
                    copyFile(historyFile, historyFileCopy);
                    try {
                        resultSet = getQueryResultSetByDateFrom(dateFromString);
                        while (resultSet.next()) {
                            String line = "URL " + resultSet.getString("url") + ", Visited On " +  resultSet.getString("local_last_visit_time") + '\n';
                            log.info(line);
                            Files.write(Paths.get(this.generalFolderFullPath, "ResultHistory", fileName), line.getBytes(), new StandardOpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.APPEND});
                        }
                    } catch (IOException e) {
                        log.severe("Error in writing: " + e.getMessage());
                    } catch (ClassNotFoundException e) {
                        log.severe("Error setting jdbc driver: " + e.getMessage());
                    } catch (SQLException e) {
                        log.severe("Error executing sql: " + e.getMessage());
                    } finally {
                        try {
                            resultSet.close();
                            statement.close();
                            connection.close();
                            if (!historyFileCopy.delete()) {
                                log.info("historyFileCopy is not deleted");
                            }
                        } catch (SQLException ex) {
                            log.severe("Error in finally block: " + ex.getMessage());
                        }
                    }

                    Thread.sleep(duration);
                } catch (InterruptedException e) {
                    log.severe("Error in thread: " + e.getMessage());
                }
            }
        });
        run.start();
    }

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

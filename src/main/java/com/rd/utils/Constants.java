package com.rd.utils;

public class Constants {
    public static final String CHROME_BROWSER_TYPE = "chrome";
    public static final String FIREFOX_BROWSER_TYPE = "firefox";
    public static final String HOME_PATH = System.getProperty("user.home");
    public static final String CHROME_DB_PATH_POSTFIX = "\\AppData\\Local\\Google\\Chrome\\User Data\\Default\\History";
    public static final String CHROME_DB_COPY_PATH_POSTFIX = "\\AppData\\Local\\Google\\Chrome\\User Data\\Default\\HistoryCopy";
    public static final String CHROME_DB_COPY_FILE_NAME = "HistoryCopy";
    public static final String FIREFOX_PROFILES_PATH_POSTFIX = "\\AppData\\Roaming\\Mozilla\\Firefox\\Profiles\\";
    public static final String FIREFOX_DB_FILE_NAME = "places.sqlite";
    public static final String FIREFOX_DB_COPY_FILE_NAME = "placesCopy.sqlite";
    // TODO: set FIREFOX_FOLDER_MASK as input parameter
    public static final String FIREFOX_FOLDER_MASK_ESR = "*.default-esr";
    public static final String FIREFOX_FOLDER_MASK_RELEASE = "*.default-release";
    public static final String COPY = "Copy";
    public static final String JDBC_SQLITE = "jdbc:sqlite:";
}

package com.rd;

import com.rd.models.ConfigRecord;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static com.rd.utils.Constants.*;

public class Main {

    private static final Logger logger = Logger.getLogger(HistoryCopyMaker.class.getName());

    private static final String CURRENT_USERNAME = System.getProperty("user.name").toLowerCase();

    public static void main(String... args) {
        logger.info("Program (main function) has been started at: " + (new GregorianCalendar()).toZonedDateTime());
        logger.info("PROGRAM PARAMETERS, Args: " + Arrays.asList(args));

        try {
            if (args.length < 2) {
                logger.info("Invalid arguments. Please provide working directory path and config path.");
                System.exit(-1);
            }

            String workingDirectoryPath = args[0];
            String configPath = args[1];

            Map<String, ConfigRecord> config = readConfigFile(configPath);

            if (config.containsKey(CURRENT_USERNAME)) {
                ConfigRecord configRecord = config.get(CURRENT_USERNAME);

                String browserType = configRecord.getBrowserType();
                String pathToDb = configRecord.getPath();
                boolean isLogging = configRecord.isLogging();

                String dbPathString = getDbPathString(browserType, pathToDb);
                String dbCopyPathString = getCopyDbPathString(dbPathString);

                logger.info("Input parameters: \n" +
                        "currentUsername=" + CURRENT_USERNAME + ",\n" +
                        "workingDirectoryPath=" + workingDirectoryPath + ",\n" +
                        "browserType=" + browserType + ",\n" +
                        "isLogging=" + isLogging + ",\n" +
                        "dbPathString=" + dbPathString + ",\n" +
                        "dbCopyPathString=" + dbCopyPathString);

                HistoryCopyMaker historyCopyMaker = new HistoryCopyMaker(
                        workingDirectoryPath,
                        browserType,
                        isLogging,
                        CURRENT_USERNAME,
                        dbPathString,
                        dbCopyPathString
                );
                historyCopyMaker.startProcess();
            } else {
                logger.info("Username " + CURRENT_USERNAME + " not found in config file. Exited.");
                System.exit(0);
            }

            logger.info("Program (main function) finished");
        } catch (Exception e) {
            logger.severe("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private static Map<String, ConfigRecord> readConfigFile(String jsonConfigPath) {
        Map<String, ConfigRecord> configMap = new HashMap<>();

        try (InputStream inputStream = Files.newInputStream(Paths.get(jsonConfigPath))) {
            JsonReader jsonReader = Json.createReader(inputStream);
            JsonArray jsonArray = jsonReader.readArray();

            for (JsonObject jsonObject : jsonArray.getValuesAs(JsonObject.class)) {
                String usernameConfig = jsonObject.getString("username", null).toLowerCase();

                if (StringUtils.isBlank(usernameConfig)) {
                    throw new IllegalArgumentException("Invalid username in config file");
                }

                String browserTypeConfig = jsonObject.getString("browserType", FIREFOX_BROWSER_TYPE);
                String pathConfig = jsonObject.getString("path", "");
                boolean loggingConfig = jsonObject.getBoolean("logging", false);

                ConfigRecord configRecord = new ConfigRecord(usernameConfig, browserTypeConfig, pathConfig, loggingConfig);
                configMap.put(usernameConfig, configRecord);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading config file: " + e.getMessage(), e);
        } catch (jakarta.json.JsonException e) {
            throw new RuntimeException("Error parsing JSON in config file: " + e.getMessage(), e);
        }

        return configMap;
    }

    private static String getDbPathString(String browserType, String dbPathOverwritten) {
        String dbPathString = dbPathOverwritten;

        boolean isDefaultValue = StringUtils.isBlank(dbPathOverwritten);
        if (isDefaultValue) {
            logger.info("Taking default value for history path.");
            switch (browserType.toLowerCase()) {
                case FIREFOX_BROWSER_TYPE:
                    String fullPathToFirefoxProfilesString = Paths.get(HOME_PATH, FIREFOX_PROFILES_PATH_POSTFIX).normalize().toString();
                    File rootProfilesFirefoxDirectory = new File(fullPathToFirefoxProfilesString);
                    String[] firefoxFilesNames = rootProfilesFirefoxDirectory.list(new WildcardFileFilter(FIREFOX_FOLDER_MASK_RELEASE));

                    String firefoxDBFolder;
                    if (firefoxFilesNames != null && firefoxFilesNames.length > 0) {
                        logger.info("Firefox folder was found by mask " + FIREFOX_FOLDER_MASK_RELEASE);
                        firefoxDBFolder = firefoxFilesNames[0];
                    } else {
                        logger.severe(fullPathToFirefoxProfilesString + " doesn't contain folder by mask.");
                        throw new RuntimeException("No firefox folder was found by mask!");
                    }

                    dbPathString = Paths.get(HOME_PATH, FIREFOX_PROFILES_PATH_POSTFIX, firefoxDBFolder, FIREFOX_DB_FILE_NAME).normalize().toString();
                    break;
                case CHROME_BROWSER_TYPE:
                    dbPathString = Paths.get(HOME_PATH, CHROME_DB_PATH_POSTFIX).normalize().toString();
                    break;
            }
        } else {
            logger.info("Taking overwritten value for history path.");
        }

        logger.info("Defined dbCopyPathString = " + dbPathString);

        return dbPathString;
    }

    private static String getCopyDbPathString(String path) {
        String dbCopyPathString = "";

        logger.info("Generating copy history path");
        if (StringUtils.isNotBlank(path)) {
            int lastBackslashIndex = StringUtils.lastIndexOf(path, "\\");

            if (lastBackslashIndex != -1) {
                String pathBeforeDbHistory = StringUtils.substring(path, 0, lastBackslashIndex + 1);
                String dbHistoryName = StringUtils.substringAfterLast(path, "\\");
                String updatedDbHistoryName = Character.toUpperCase(dbHistoryName.charAt(0)) + dbHistoryName.substring(1);
                String copyDbHistoryName = COPY + updatedDbHistoryName;
                dbCopyPathString = pathBeforeDbHistory + copyDbHistoryName;
            } else {
                throw new RuntimeException("Path to db history doesn't contain double backslash symbol ('\\')!");
            }
        }

        logger.info("Defined dbCopyPathString = " + dbCopyPathString);

        return dbCopyPathString;
    }
}

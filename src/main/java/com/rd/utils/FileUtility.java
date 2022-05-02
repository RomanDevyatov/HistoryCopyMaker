package com.rd.utils;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.logging.Logger;

public class FileUtility {

    private static final Logger log = Logger.getLogger(FileUtility.class.getName());

    protected String fileName;


    protected void copyFile(File source, File dest) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(source);
            os = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer, 0, 1024)) > 0) {
                os.write(buffer, 0, length);
            }
        } catch (IOException e) {
            log.severe("Error in copying history file: " + e.getMessage());
        } finally {
            is.close();
            os.close();
        }
    }

    protected void createResFile(String path, String userName) {
        path += "/ResultHistory";
        File pathDir = new File(path);

        if (!pathDir.exists()) {
            log.info("Directory " + path + " doesn't exist");
            pathDir.mkdirs();
        } else {
            log.info("Directory " + path + " exists, OK");
        }

        this.fileName = userName + "_historyRes_" + LocalDate.now() + ".txt";
        log.info("Generated history file name: " + this.fileName);

        Path filePath = Paths.get(path, this.fileName);
        if (!Files.exists(filePath)) {
            try {
                Files.createFile(filePath);
                log.info("New file history is created: " + this.fileName);
            } catch (IOException e) {
                log.severe("Error in file creation: " + e.getMessage());
            }
        } else {
            log.info("File " + filePath + " already exists");
        }
    }

    protected boolean removeFile(String path) {
        File file = new File(path);
        return file.delete();
    }

}

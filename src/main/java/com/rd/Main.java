package com.rd;

public class Main {

    public static void main(String[] args) {
        try {
            String generalFolderPath = args[0];
            ChromeHistoryCopyMaker chromeHistoryVisitAnalyzer = new ChromeHistoryCopyMaker(generalFolderPath);
            chromeHistoryVisitAnalyzer.startProcess();
        } catch (ArrayIndexOutOfBoundsException e) {
            System.exit(-2);
        }
    }
}

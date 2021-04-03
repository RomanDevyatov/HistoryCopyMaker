package com.rd;

public class Main {

    public static void main(String[] args) {
        String generalFolderPath = args[0];
        ChromeHistoryCopyMaker chromeHistoryVisitAnalyzer = new ChromeHistoryCopyMaker(generalFolderPath);
        chromeHistoryVisitAnalyzer.startProcess();
    }
}

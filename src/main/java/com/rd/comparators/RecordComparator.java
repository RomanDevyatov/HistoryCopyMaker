package com.rd.comparators;

import com.rd.models.HistoryRecord;

import java.util.Comparator;

public class RecordComparator implements Comparator<HistoryRecord> {
    public int compare(HistoryRecord hr, HistoryRecord hrAnother) {
        return hr.getDate().compareTo(hrAnother.getDate());
    }
}

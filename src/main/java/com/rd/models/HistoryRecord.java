package com.rd.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class HistoryRecord {
    public static final String HISTORY_RECORD_DELEMITER = ", Visited On ";

    private String url;
    private String date;

    @Override
    public String toString() {
        return url + HISTORY_RECORD_DELEMITER + date;
    }
}

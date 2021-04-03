package com.rd;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.atomic.AtomicLong;

public class DateUtility {

    private AtomicLong getMilTimeFrom7MorningOfToday() {
        LocalDate date = LocalDate.now();
        Calendar calendarStart = new GregorianCalendar(date.getYear(), date.getMonth().getValue() - 1, date.getDayOfMonth(), 7, 0, 0);
        return new AtomicLong(calendarStart.getTimeInMillis());
    }
}

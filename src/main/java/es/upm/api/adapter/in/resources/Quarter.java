package es.upm.api.adapter.in.resources;

import es.upm.miw.exception.BadRequestException;

import java.time.LocalDate;

public enum Quarter {
    T1(1, 3),
    T2(4, 6),
    T3(7, 9),
    T4(10, 12);

    private final int firstMonth;
    private final int lastMonth;

    Quarter(int firstMonth, int lastMonth) {
        this.firstMonth = firstMonth;
        this.lastMonth = lastMonth;
    }

    public LocalDate fromDate(int year) {
        return LocalDate.of(year, this.firstMonth, 1);
    }

    public LocalDate toDate(int year) {
        LocalDate firstDayOfLastMonth = LocalDate.of(year, this.lastMonth, 1);
        return firstDayOfLastMonth.withDayOfMonth(firstDayOfLastMonth.lengthOfMonth());
    }

    public static Quarter from(String value) {
        for (Quarter quarter : values()) {
            if (quarter.name().equalsIgnoreCase(value)) {
                return quarter;
            }
        }
        throw new BadRequestException("Quarter must be one of T1, T2, T3, T4: " + value);
    }

    public static Quarter from(LocalDate date) {
        for (Quarter quarter : values()) {
            if (date.getMonthValue() >= quarter.firstMonth && date.getMonthValue() <= quarter.lastMonth) {
                return quarter;
            }
        }
        throw new BadRequestException("Invalid quarter date: " + date);
    }
}

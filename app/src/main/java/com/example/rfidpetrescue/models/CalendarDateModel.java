package com.example.rfidpetrescue.models;

import java.util.Date;

public class CalendarDateModel {
    private Date date;
    private boolean isToday;
    private boolean isPast;

    public CalendarDateModel(Date date, boolean isToday, boolean isPast) {
        this.date = date;
        this.isToday = isToday;
        this.isPast = isPast;
    }

    public Date getDate() { return date; }
    
    public boolean isToday() { return isToday; }
    
    public void setToday(boolean today) {
        isToday = today;
    }

    public boolean isPast() { return isPast; }
    
    public void setPast(boolean past) {
        isPast = past;
    }
}
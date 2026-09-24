package com.example.rfidpetrescue.models;

public class TimeSlot {
    private String time;
    private boolean isSelected;

    public TimeSlot(String time) {
        this.time = time;
        this.isSelected = false;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}
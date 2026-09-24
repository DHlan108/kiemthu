package com.example.rfidpetrescue.models;

public class ScheduleTask {
    private String id;
    private String title;
    private String description;
    private String time;
    private String date;
    private String type;
    
    // Các trường bổ sung để hiện rõ thông tin
    private String petName;
    private String adopterName;
    private String vaccineType;
    
    // Các ID để điều hướng
    private String petId;
    private String targetId; // Dùng cho Appointment ID hoặc các ID khác
    private long timestamp;
    private boolean manual;

    public ScheduleTask() {}

    public ScheduleTask(String id, String title, String description, String time, String date, String type) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.time = time;
        this.date = date;
        this.type = type;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public long getTimestamp() {
        return timestamp;
    }
    public void setDescription(String description) { this.description = description; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getPetName() { return petName; }
    public void setPetName(String petName) { this.petName = petName; }
    public String getAdopterName() { return adopterName; }
    public void setAdopterName(String adopterName) { this.adopterName = adopterName; }
    public String getVaccineType() { return vaccineType; }
    public void setVaccineType(String vaccineType) { this.vaccineType = vaccineType; }

    public String getPetId() { return petId; }
    public void setPetId(String petId) { this.petId = petId; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isManual() {
        return manual;
    }

    public void setManual(boolean manual) {
        this.manual = manual;
    }
}
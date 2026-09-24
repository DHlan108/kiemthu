package com.example.rfidpetrescue.models;
public class Campaign {
    public String id, title, sub_title, description, usage_purpose, image_url, start_date, end_date;
    public long target_amount, current_amount;
    private String status;

    public Campaign() {} // Bắt buộc phải có cho Firebase

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public String getSub_title() { return sub_title; }
    public String getDescription() { return description; }
    public String getUsage_purpose() { return usage_purpose; }
    
    public long getCurrent_amount() { return current_amount; }
    public void setCurrent_amount(long current_amount) { this.current_amount = current_amount; }
    
    public long getTarget_amount() { return target_amount; }
    public void setTarget_amount(long target_amount) { this.target_amount = target_amount; }

    public String getImage_url() { return image_url; }
    public String getStart_date() { return start_date; }
    public String getEnd_date() { return end_date; }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
package com.example.rfidpetrescue.models;

public class RFIDTag {
    private String id;
    private String uid;
    private String tag_type;
    private String assigned_date;
    private boolean is_active;
    private String current_pet_id;

    public RFIDTag() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getTag_type() {
        return tag_type;
    }

    public void setTag_type(String tag_type) {
        this.tag_type = tag_type;
    }

    public String getAssigned_date() {
        return assigned_date;
    }

    public void setAssigned_date(String assigned_date) {
        this.assigned_date = assigned_date;
    }

    public boolean isIs_active() {
        return is_active;
    }

    public void setIs_active(boolean is_active) {
        this.is_active = is_active;
    }

    public String getCurrent_pet_id() {
        return current_pet_id;
    }

    public void setCurrent_pet_id(String current_pet_id) {
        this.current_pet_id = current_pet_id;
    }

    // Tạo Getter/Setter
}

package com.example.rfidpetrescue.models;

public class Vaccination {
    public String id;
    public String pet_id;
    public String vaccine_type;    // Loại tiêm: Ngừa dại, Ngừa bệnh...
    public String medicine_name;   // Tên thuốc
    public String vaccine_date;    // Ngày tiêm
    public String revaccine_date;  // Ngày tái chủng
    public String note;
    public long created_at;

    public Vaccination() {}

    public Vaccination(String id, String pet_id, String vaccine_type, String medicine_name,
                       String vaccine_date, String revaccine_date, String note, long created_at) {
        this.id = id;
        this.pet_id = pet_id;
        this.vaccine_type = vaccine_type;
        this.medicine_name = medicine_name;
        this.vaccine_date = vaccine_date;
        this.revaccine_date = revaccine_date;
        this.note = note;
        this.created_at = created_at;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPet_id() { return pet_id; }
    public void setPet_id(String pet_id) { this.pet_id = pet_id; }
    public String getVaccine_type() { return vaccine_type; }
    public void setVaccine_type(String vaccine_type) { this.vaccine_type = vaccine_type; }
    public String getMedicine_name() { return medicine_name; }
    public void setMedicine_name(String medicine_name) { this.medicine_name = medicine_name; }
    public String getVaccine_date() { return vaccine_date; }
    public void setVaccine_date(String vaccine_date) { this.vaccine_date = vaccine_date; }
    public String getRevaccine_date() { return revaccine_date; }
    public void setRevaccine_date(String revaccine_date) { this.revaccine_date = revaccine_date; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public long getCreated_at() { return created_at; }
    public void setCreated_at(long created_at) { this.created_at = created_at; }
}
package com.example.rfidpetrescue.models;

public class MedicalRecord {
    public String id;
    public String pet_id;
    public String record_type;
    public String date;
    public String symptoms;
    public String clinical_signs;
    public String diagnosis;
    public String note;
    public long created_at;

    // Constructor rỗng bắt buộc cho Firebase
    public MedicalRecord() {
    }

    public MedicalRecord(String id, String pet_id, String record_type, String date, String symptoms, String clinical_signs, String diagnosis, String note, long created_at) {
        this.id = id;
        this.pet_id = pet_id;
        this.record_type = record_type;
        this.date = date;
        this.symptoms = symptoms;
        this.clinical_signs = clinical_signs;
        this.diagnosis = diagnosis;
        this.note = note;
        this.created_at = created_at;
    }

    // --- Getter và Setter ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPet_id() { return pet_id; }
    public void setPet_id(String pet_id) { this.pet_id = pet_id; }

    public String getRecord_type() { return record_type; }
    public void setRecord_type(String record_type) { this.record_type = record_type; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getSymptoms() { return symptoms; }
    public void setSymptoms(String symptoms) { this.symptoms = symptoms; }

    public String getClinical_signs() { return clinical_signs; }
    public void setClinical_signs(String clinical_signs) { this.clinical_signs = clinical_signs; }

    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public long getCreated_at() { return created_at; }
    public void setCreated_at(long created_at) { this.created_at = created_at; }
}
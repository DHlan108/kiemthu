package com.example.rfidpetrescue.models;

public class Appointment {

    private String id;
    private String userId;
    private String petId;
    private String adopterName;
    private String adopterPhone;
    private String adopterEmail;
    private String date;
    private String time;
    private String session; // Sáng (Morning) hay Chiều (Afternoon)
    private String location;
    private String status; // Pending, Confirmed, Cancelled
    private String createdAt;

    public Appointment() {
    }

    public Appointment(String userId, String petId, String adopterName, String adopterPhone,
                       String adopterEmail, String date, String time, String session,
                       String location, String status, String createdAt) {
        this.userId = userId;
        this.petId = petId;
        this.adopterName = adopterName;
        this.adopterPhone = adopterPhone;
        this.adopterEmail = adopterEmail;
        this.date = date;
        this.time = time;
        this.session = session;
        this.location = location;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getPetId() { return petId; }
    public void setPetId(String petId) { this.petId = petId; }

    public String getAdopterName() { return adopterName; }
    public void setAdopterName(String adopterName) { this.adopterName = adopterName; }

    public String getAdopterPhone() { return adopterPhone; }
    public void setAdopterPhone(String adopterPhone) { this.adopterPhone = adopterPhone; }

    public String getAdopterEmail() { return adopterEmail; }
    public void setAdopterEmail(String adopterEmail) { this.adopterEmail = adopterEmail; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getSession() { return session; }
    public void setSession(String session) { this.session = session; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
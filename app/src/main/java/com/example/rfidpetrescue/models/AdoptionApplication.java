package com.example.rfidpetrescue.models;

public class AdoptionApplication {
    private String id;
    private String adopterEmail;
    private String adopterName;
    private String adopterPhone;
    private String adopterAddress;
    private String housingType;
    private String petExperience;
    private String hasChildren;
    private String occupation;
    private String reasonAdoption;
    private String createdAt;
    private String date;
    private String time;
    private String location;
    private String petId;
    private String status;
    private String userId;

    public AdoptionApplication() {
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAdopterEmail() { return adopterEmail; }
    public void setAdopterEmail(String adopterEmail) { this.adopterEmail = adopterEmail; }

    public String getAdopterName() { return adopterName; }
    public void setAdopterName(String adopterName) { this.adopterName = adopterName; }

    public String getAdopterPhone() { return adopterPhone; }
    public void setAdopterPhone(String adopterPhone) { this.adopterPhone = adopterPhone; }

    public String getAdopterAddress() { return adopterAddress; }
    public void setAdopterAddress(String adopterAddress) { this.adopterAddress = adopterAddress; }

    public String getHousingType() { return housingType; }
    public void setHousingType(String housingType) { this.housingType = housingType; }

    public String getPetExperience() { return petExperience; }
    public void setPetExperience(String petExperience) { this.petExperience = petExperience; }

    public String getHasChildren() { return hasChildren; }
    public void setHasChildren(String hasChildren) { this.hasChildren = hasChildren; }

    public String getOccupation() { return occupation; }
    public void setOccupation(String occupation) { this.occupation = occupation; }

    public String getReasonAdoption() { return reasonAdoption; }
    public void setReasonAdoption(String reasonAdoption) { this.reasonAdoption = reasonAdoption; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getPetId() { return petId; }
    public void setPetId(String petId) { this.petId = petId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}
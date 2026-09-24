package com.example.rfidpetrescue.models;

import com.google.firebase.database.PropertyName;

public class Pet {
    private String id;
    private String name;
    private String species;
    private String breed;
    private String gender;
    private String age;
    private Double weight;
    private String color;
    private String living_environment;
    private String note;
    private String status;
    private String rfid_tag_id;
    private String health_profile_id;
    private String created_at;
    private String imageUrl;
    private String location;
    private String aboutDesc;
    private String isVaccinated;
    private String rescueHistory;
    private Boolean sterilized;
    private String owner_id;


    // Constructor rỗng bắt buộc cho Firebase
    public Pet() { }

    // --- Getter và Setter ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSpecies() { return species; }
    public void setSpecies(String species) { this.species = species; }

    public String getBreed() { return breed; }
    public void setBreed(String breed) { this.breed = breed; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getAge() { return age; }
    public void setAge(String age) { this.age = age; }

    public Double getWeight() { return weight; }
    public void setWeight(Object weight) {
        if (weight instanceof String) {
            try {
                this.weight = Double.parseDouble((String) weight);
            } catch (NumberFormatException e) {
                this.weight = 0.0;
            }
        } else if (weight instanceof Number) {
            this.weight = ((Number) weight).doubleValue();
        } else {
            this.weight = 0.0;
        }
    }

    // BỔ SUNG: Getter/Setter cho Color
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    // BỔ SUNG: Getter/Setter cho Living Environment
    @PropertyName("living_environment")
    public String getLiving_environment() { return living_environment; }
    @PropertyName("living_environment")
    public void setLiving_environment(String living_environment) { this.living_environment = living_environment; }

    // BỔ SUNG: Getter/Setter cho Note
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRfid_tag_id() { return rfid_tag_id; }
    public void setRfid_tag_id(String rfid_tag_id) { this.rfid_tag_id = rfid_tag_id; }

    public String getHealth_profile_id() { return health_profile_id; }
    public void setHealth_profile_id(String health_profile_id) { this.health_profile_id = health_profile_id; }

    public String getCreated_at() { return created_at; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getAboutDesc() { return aboutDesc; }
    public void setAboutDesc(String aboutDesc) { this.aboutDesc = aboutDesc; }

    public String getIsVaccinated() { return isVaccinated; }
    public void setIsVaccinated(String isVaccinated) { this.isVaccinated = isVaccinated; }
    public Boolean isSterilized() {
        return sterilized;
    }
    public void setSterilized(boolean sterilized) { this.sterilized = sterilized; }

    public String getRescueHistory() { return rescueHistory; }

    public void setRescueHistory(String rescueHistory) { this.rescueHistory = rescueHistory; }

    public String getOwner_id() { return owner_id; }
    public void setOwner_id(String owner_id) { this.owner_id = owner_id; }
}
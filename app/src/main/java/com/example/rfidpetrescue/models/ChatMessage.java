package com.example.rfidpetrescue.models;

import com.google.firebase.database.PropertyName;

public class ChatMessage {
    public static final String TYPE_TEXT = "text";
    public static final String TYPE_IMAGE = "image";
    public static final String TYPE_PET_CARD = "pet_card";

    @PropertyName("sender_id")
    private String sender_id;
    @PropertyName("text")
    private String text;
    @PropertyName("timestamp")
    private Long timestamp;
    @PropertyName("imageUrl")
    private String imageUrl;
    @PropertyName("type")
    private String type = TYPE_TEXT;

    // Các trường dành cho tin nhắn loại pet_card
    private String petId;
    private String petName;
    private String petBreed;
    private String petImage;

    public ChatMessage() {}

    // Constructor cho tin nhắn văn bản
    public ChatMessage(String sender_id, String text, Long timestamp) {
        this.sender_id = sender_id;
        this.text = text;
        this.timestamp = timestamp;
        this.type = TYPE_TEXT;
    }

    // Constructor cho tin nhắn hình ảnh
    public ChatMessage(String sender_id, String text, Long timestamp, String imageUrl) {
        this.sender_id = sender_id;
        this.text = text;
        this.timestamp = timestamp;
        this.imageUrl = imageUrl;
        this.type = TYPE_IMAGE;
    }

    // Constructor cho thẻ thú cưng (Kiểu Shopee)
    public ChatMessage(String sender_id, String petId, String petName, String petBreed, String petImage, Long timestamp) {
        this.sender_id = sender_id;
        this.petId = petId;
        this.petName = petName;
        this.petBreed = petBreed;
        this.petImage = petImage;
        this.timestamp = timestamp;
        this.type = TYPE_PET_CARD;
        this.text = "[Thẻ thú cưng: " + petName + "]";
    }

    @PropertyName("sender_id")
    public String getSender_id() { return sender_id; }
    @PropertyName("sender_id")
    public void setSender_id(String sender_id) { this.sender_id = sender_id; }

    @PropertyName("text")
    public String getText() { return text; }
    @PropertyName("text")
    public void setText(String text) { this.text = text; }

    @PropertyName("timestamp")
    public Long getTimestamp() { return timestamp; }
    @PropertyName("timestamp")
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }

    @PropertyName("imageUrl")
    public String getImageUrl() { return imageUrl; }
    @PropertyName("imageUrl")
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    @PropertyName("type")
    public String getType() { return type; }
    @PropertyName("type")
    public void setType(String type) { this.type = type; }

    public String getPetId() { return petId; }
    public void setPetId(String petId) { this.petId = petId; }

    public String getPetName() { return petName; }
    public void setPetName(String petName) { this.petName = petName; }

    public String getPetBreed() { return petBreed; }
    public void setPetBreed(String petBreed) { this.petBreed = petBreed; }

    public String getPetImage() { return petImage; }
    public void setPetImage(String petImage) { this.petImage = petImage; }
}

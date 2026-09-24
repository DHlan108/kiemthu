package com.example.rfidpetrescue.models;

import com.google.firebase.database.PropertyName;

public class Donate {
    private long amount;
    private String content;
    private String status;
    private String timestamp;
    private String type;

    @PropertyName("campaign_id")
    private String campaignId;

    @PropertyName("user_name")
    private String userName;

    // 1. Constructor trống là BẮT BUỘC cho Firebase
    public Donate() {}

    // 2. Cần thêm Setters để Firebase có thể đổ dữ liệu vào object
    public long getAmount() { return amount; }
    public void setAmount(long amount) { this.amount = amount; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    @PropertyName("campaign_id")
    public String getCampaignId() { return campaignId; }

    @PropertyName("campaign_id")
    public void setCampaignId(String campaignId) { this.campaignId = campaignId; }

    @PropertyName("user_name")
    public String getUserName() { return userName; }

    @PropertyName("user_name")
    public void setUserName(String userName) { this.userName = userName; }
}
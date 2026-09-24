package com.example.rfidpetrescue.models;

public class ChatChannel {
    private String roomId;
    private String name;
    private String lastMessage;
    private String time;
    private String avatarUrl;
    private boolean unread;

    public ChatChannel(String roomId, String name, String lastMessage, String time, String avatarUrl, boolean unread) {
        this.roomId = roomId;
        this.name = name;
        this.lastMessage = lastMessage;
        this.time = time;
        this.avatarUrl = avatarUrl;
        this.unread = unread;
    }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public boolean isUnread() { return unread; }
    public void setUnread(boolean unread) { this.unread = unread; }
}

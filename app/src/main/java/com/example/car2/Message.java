package com.example.car2;

import com.google.firebase.Timestamp;

public class Message {
    private String senderId;
    private String text;
    private Timestamp timestamp;

    public Message() {}

    public Message(String senderId, String text, Timestamp timestamp) {
        this.senderId = senderId;
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getSenderId() { return senderId; }
    public String getText() { return text; }
    public Timestamp getTimestamp() { return timestamp; }
}

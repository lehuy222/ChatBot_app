package com.example.ass8;

class Message {
    private String text;
    private boolean isSender;

    Message(String text, boolean isSender) {
        this.text = text;
        this.isSender = isSender;
    }

    String getText() {
        return text;
    }

    boolean isSender() {
        return isSender;
    }
}

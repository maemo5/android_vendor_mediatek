package com.mediatek.rcs.pam.model;

public class EventType {
    public static final int TYPE_DEFAULT = 0;
    public static final int TYPE_MENU_CLICKED = 0; // min
    public static final int TYPE_LINK_CLICKED = 1;
    public static final int TYPE_TERMINAL_API_CLICKED = 2;
    public static final int TYPE_APP_CLICKED = 3; // max

    public int type;
    public String key;

    public EventType(int type, String key) {
        this.type = type;
        this.key = key;
    }

    public EventType(String type, String key) {
        this.type = Integer.parseInt(type);
        if (this.type > TYPE_APP_CLICKED || this.type < TYPE_MENU_CLICKED) {
            throw new Error("Invalid type: " + type);
        }
        this.key = key;
    }
}

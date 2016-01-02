package com.cmcc.ccs.chat;

import android.content.Context;

import org.gsma.joyn.JoynService;
import org.gsma.joyn.JoynServiceListener;

import java.util.Set;

public class ChatService extends JoynService {
    public static final int SMS = 1;
    public static final int MMS = 2;
    public static final int IM = 3;
    public static final int FT = 4;
    public static final int XML = 5;

    public ChatService(Context context, JoynServiceListener listener) {
        super(context, listener);
    }

    public void addEventListener(ChatListener listener) {
    }

    public void removeChatListener(ChatListener listener) {
    }

    public String sendMessage(String contact, String message) {
        return null;
    }

    public ChatMessage getChatMessage(String msgId) {
        return null;
    }

    public String sendOTMMessage(Set<String> contacts, String message) {
        return null;
    }

    public String resendMessage(String msgId) {
        return null;
    }

    public boolean deleteMessage(String msgId) {
        return false;
    }

    public boolean setMessageRead(String msgId) {
        return false;
    }

    public boolean setMessageFavorite(String msgId) {
        return false;
    }

    public boolean moveMessageToInbox(String msgId) {
        return false;
    }

    @Override
    public void connect() {
        // TODO Auto-generated method stub

    }

    @Override
    public void disconnect() {
        // TODO Auto-generated method stub

    }
}

package com.cmcc.ccs.chat;

public interface ChatListener {
    void onNewChatMessage(String contact, ChatMessage message);

    void onReportMessageDelivered(long msgId);

    void onReportMessageFailed(long msgId, int errType, String statusCode);
}

package com.mediatek.rcs.common;

public interface IInitGroupListener {

    void onInitGroupResult(int result, long threadId, String chatId);
    void onAcceptGroupInvitationResult(int result, long threadId, String chatId);
    void onRejectGroupInvitationResult(int result, long threadId, String chatId);
}

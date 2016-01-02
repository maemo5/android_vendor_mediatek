package com.mediatek.rcs.common.service;

import java.util.List;
import com.mediatek.rcs.common.service.IRCSChatServiceListener;
import com.mediatek.rcs.common.service.Participant;

/**
 * @author mtk80881 Chat Service AIDL
 */
interface IRCSChatService {
   /**
     * @return
     */
    boolean getRCSStatus();
   /**
     * @return
     */
    boolean getConfigurationStatus();
   /**
     * @return
     */
    boolean getRegistrationStatus();

    void getBurnMessageCapability(in String contact);

    void sendBurnDeliveryReport(in String contact, in String msgId);

    String getMSISDN();
    /**
     * @param contact
     * @param content
     * @return
     */
    void sendOne2OneMessage(in String contact, in String content);

    /**
     *
     */
    void sendEmoticonShopMessage(in String contact, in String content);

    /**
     * @param contacts
     * @param content
     * @return
     */
    void sendOne2MultiMessage(in List<String> contacts, in String content);
    //need discuss with stack if save message in one DB
    
    /**
     * @param contact
     * @param content
     * @return
     */
    void sendO2MEmoticonShopMessage(in List<String> contacts, in String content);

    /**
     * @param contact
     * @param content
     * @return
     */
    void sendBurnMessage(in String contact, in String content);

    /**
     * @param contact
     * @param filePath
     * @return
     */
    void sendOne2OneFileTransfer(in String contact, in String filePath);

    /**
     * @param String
     */
    void sendOne2OneBurnFileTransfer(in String contact, in String filePath);

    /**
     * @param contacts
     * @param filePath
     * @return
     */
    void sendOne2MultiFileTransfer(in List<String> contacts, in String filePath);

    /**
     * 
     */
    void resendFileTransfer(in long index);

    void sendGroupFileTransfer(in String chatId, in String filePath);
    
    void resendGroupFileTransfer(in String chatId, in long msgId);

    /**
     * 
     */
    void resendOne2MultiFileTransfer(in long index);

    /**
     * 
     */
    void acceptFileTransfer(in String fileTransferTag);

    /**
     * 
     */
    void acceptGroupFileTransfer(in String chatId, in String fileTransferTag);
    
    /**
     * 
     */
    void reAcceptFileTransfer(in String fileTransferTag,in long IpMsgId);
    
    /**
     * 
     */
    void reAcceptGroupFileTransfer(in String chatId, in String fileTransferTag, in long ipMessageId);
    
    /**
     * 
     */
    void resumeFileTransfer();
    /**
     * 
     */
    long getRcsFileTransferMaxSize();

    void pauseFileTransfer(in String fileTransferTag);

    /**
     * @param index
     * @return
     */
    void resendMessage(in String contact, in long index);

    /**
     * 
     */
    void resendMultiMessage(in long index);
    
    /**
     * 
     */
    void resendGroupMessage(in String chatId, in long index);    

    /**
     * @param index
     */
    void deleteMessage(in long index);

    /**
     * @param indexs
     */
    void deleteMessages(in long[] indexs);

    /**
     * @param contact
     */
    void deleteO2OMessages(in String contact);

    /**
     * @param chatId
     */
    void deleteGroupMessages(in String chatId);

    /**
     * @param List
     */
    void startGroups(in List<String> chatIds);

    /**
     * @param List
     * @return
     */
    String initGroupChat(in String subject, in List<String> contacts);

    void acceptGroupChat(in String chatId);
    
    void rejectGroupChat(in String chatId);
    
    List<String> getGroupParticipants(in String chatId);
    /**
     * @param String
     * @return
     */
    void sendGroupMessage(in String chatId, in String content); 
    
    /**
     * @param String
     * @param String
     * @return
     */
    void sendGroupEmotionShopMessage(in String chatId, in String content);

    /**
     * @param String
     * @return
     */
    void addParticipants(in String chatId, in List<Participant> participants);
    
    /**
     * @param String
     * @return
     */
    void removeParticipants(in String chatId, in List<Participant> participants);
    
    /**
     * @param String
     * @return
     */
    void modifySubject(in String chatId, in String subject);
    
    /**
     * @param String
     * @return
     */
    void modifyNickName(in String chatId, in String nickName);
    
    /**
     * @param String
     * @return
     */
    void modifyRemoteAlias(in String chatId, in String alias);
    
    /**
     * @param String
     * @return
     */
    void transferChairman(in String chatId, in String contact);
    
    /**
     * @param String
     * @return
     */
    void quit(in String chatId);
    
    /**
     * @param String
     * @return
     */
    void abort(in String chatId);
    
    /**
     * @param String
     * @return
     */
    void blockMessages(in String chatId, in boolean block);

    /**
     * @param IRCSChatServiceListener
     * @return
     */
    void addRCSChatServiceListener(IRCSChatServiceListener listener);
    
    /**
     * @param IRCSChatServiceListener
     * @return
     */
    void removeRCSChatServiceListener(IRCSChatServiceListener listener);        
}
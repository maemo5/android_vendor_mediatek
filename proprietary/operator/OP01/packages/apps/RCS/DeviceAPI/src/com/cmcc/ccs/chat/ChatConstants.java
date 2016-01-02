/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.cmcc.ccs.chat;

public class ChatConstants {

  
    /**
     * ChatService contant
     */
    public static class ChatService {
	    /**
	     * timeout
	     */
	    public static final int TIMEOUT = 0;
	    
	    /**
	     * unknown type
	     */
	    public static final int UNKNOW = 1;
	    /**
	     * internal
	     */
	    public static final int INTERNAL = 0;
	    
	    /**
	     * outofsize
	     */
	    public static final int OUTOFSIZE = 1;
    }
    
    /**
     * ChatMessage contant
     */
    public static class ChatMessage {
	    /**
	     * The message has been delivered, but we don't know if the message
	     * has been read by the remote
	     */
	    public static final int UNREAD = 0;
	
	    /**
	     * The message has been delivered and a displayed delivery report is
	     * requested, but we don't know if the message has been read by the remote
	     */
	    public static final int UNREAD_REPORT = 1;
	    
	    /**
	     * The message has been read by the remote (i.e. displayed)
	     */
	    public static final int READ = 2;
	    
	    /**
	     * The message is in progress of sending
	     */
	    public static final int SENDING = 3;
	    
	    /**
	     * The message has been sent
	     */
	    public static final int SENT = 4;
	
	    /**
	     * The message is failed to be sent
	     */
	    public static final int FAILED = 5;
	    
	    /**
	     * The message is queued to be sent by joyn service when possible
	     */
	    public static final int TO_SEND = 6;
	    
	    /**
	     * Incoming message
	     */
	    public static final int INCOMING = 7;
	    
	    /**
	     * Outgoing message
	     */
	    public static final int OUTGOING = 8;
	    
        /**
         * The name of the column containing the message ID.
         * <P>Type: TEXT</P>
         */
        public static final String MESSAGE_ID = "msg_id";
        
        /**
         * The name of the column containing the identity of the sender of the message.
         * <P>Type: TEXT</P>
         */
        public static final String CONTACT_NUMBER = "sender";
        	
        /**
         * The name of the column containing the message body.
         * <P>Type: BLOB</P>
         */
        public static final String BODY = "body";
     
        /**
         * The name of the column containing the time when message is created.
         * <P>Type: LONG</P>
         */
        public static final String TIMESTAMP = "timestamp";
        
        /**
         * The name of the column containing the MIME-TYPE of the message body.
         * <P>Type: TEXT</P>
         */
        public static final String MIME_TYPE = "mime_type";

        /**
         * The name of the column containing the message status.
         * <P>Type: INTEGER</P>
         */
        public static final String MESSAGE_STATUS = "status";
        
        /**
         * The name of the column containing the message direction.
         * <P>Type: INTEGER</P>
         */
        public static final String DIRECTION = "direction";
                
        /**
         * The name of the column containing the favorite type of message.
         * <P>Type: INTEGER</P>
         * @see ChatLog.Message.Type
         */
        public static final String FAVORITE = "favorite";
        
        /**
         * The name of the column containing the type of message.
         * <P>Type: INTEGER</P>
         * @see ChatLog.Message.Type
         */
        public static final String TYPE = "type";
        
    }
    
}

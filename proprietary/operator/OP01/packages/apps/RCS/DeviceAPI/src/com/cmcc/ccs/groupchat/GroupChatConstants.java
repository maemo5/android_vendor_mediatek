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
package com.cmcc.ccs.groupchat;




public class GroupChatConstants {


    /**
     * GroupChatService constants
     */
    public static class GroupChatService {
    	/**
    	 * Unknown state
    	 */
    	public final static int UNKNOWN = 0;

    	/**
    	 * Chat invitation received
    	 */
    	public final static int INVITED = 1;
    	
    	/**
    	 * Chat invitation sent
    	 */
    	public final static int INITIATED = 2;
    	
    	/**
    	 * Chat is started
    	 */
    	public final static int STARTED = 3;
    	
    	/**
    	 * Chat has been terminated
    	 */
    	public final static int TERMINATED = 4;
    	   	
    	/**
    	 * Chat has been aborted 
    	 */
    	public final static int ABORTED = 5;
        
    	/**
    	 * Chat has been closed by the user. A user which has closed a
    	 * conversation voluntary can't rejoin it afterward.
    	 */
    	public final static int CLOSED_BY_USER = 6;

    	/**
    	 * Chat has failed 
    	 */
    	public final static int FAILED = 7;
    	
    	/**
    	 * Chat success 
    	 */
    	public final static int OK = 8;
    	
    	/**
    	 * timeout 
    	 */
    	public final static int TIMEOUT = 9;
    	
        /**
         * internal error
         */
        public static final int INTERNAL_ERROR = 10;
    	
    	/**
    	 * Chat offline
    	 */
    	public final static int OFFLINE = 11;
    	
    	/**
    	 * Chat conversation not found
    	 */
    	public final static int CHAT_NOT_FOUND = 12;
    	
    	/**
    	 * Group chat incoming status
    	 */
    	public final static int INCOMING = 13;
    	
    	/**
    	 * Group chat outcoming status
    	 */
    	public final static int OUTCOMING = 14;
    	
    	/**
    	 * Group chat has failed
    	 */
    	public final static int CHAT_FAILED = 15;
    	
    	/**
    	 * Group chat invitation has been declined by remote
    	 */
    	public final static int INVITATION_DECLINED = 16;
    	
     
    	  /**
         * The name of the column containing the chat ID.
         * <P>Type: TEXT</P>
         */
        public static final String CHAT_ID = "chat_id";
        
        /**
         * The name of the column containing the identity of the state of the message.
         * <P>Type: TEXT</P>
         */
        public static final String STATE = "state";
        	
        /**
         * The name of the column containing the message subject.
         * <P>Type: BLOB</P>
         */
        public static final String SUBJECT = "subject";
        
        /**
         * The name of the column containing the message chairmen.
         * <P>Type: BLOB</P>
         */
        public static final String CHAIRMEN = "chairmen";
     
        /**
         * The name of the column containing the time when message is created.
         * <P>Type: LONG</P>
         */
        public static final String TIMESTAMP = "timestamp";
        
        /**
         * The name of the column containing the nick_name of the message.
         * <P>Type: TEXT</P>
         */
        public static final String NICK_NAME = "nick_name";

        /**
         * The name of the column containing the message phone number.
         * <P>Type: INTEGER</P>
         */
        public static final String PHONE_NUMBER = "phone_number";
        
        /**
         * The name of the column containing the message direction.
         * <P>Type: INTEGER</P>
         */
        public static final String DIRECTION = "direction";
                
        /**
         * The name of the column containing the protrait type of message.
         * <P>Type: INTEGER</P>
         * @see ChatLog.Message.Type
         */
        public static final String PROTRAIT = "protrait";
        
        /**
         * The name of the column containing the member name of message.
         * <P>Type: INTEGER</P>
         * @see ChatLog.Message.Type
         */
        public static final String MEMBER_NAME = "member_name";
    	
    	
    }
    
}

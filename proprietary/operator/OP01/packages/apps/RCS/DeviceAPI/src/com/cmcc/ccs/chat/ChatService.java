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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;

import com.cmcc.ccs.RCSService;
import com.cmcc.ccs.RCSServiceListener;
import com.cmcc.ccs.RCSServiceNotAvailableException;
import com.cmcc.ccs.RCSServiceRegistrationListener;

import android.util.Log;


/**
 * Chat service offers the main entry point to initiate chat 1-1 , 1-M ang group
 * conversations with contacts. Several applications may connect/disconnect
 * to the API.
 * 
 * The parameter contact in the API supports the following formats:
 * MSISDN in national or international format, SIP address, SIP-URI
 * or Tel-URI.
 * 
 */
public class ChatService extends RCSService {
	/**
	 * API
	 */
//	private org.gsma.joyn.ChatService api = null;
    private String api = null;


	public static final String TAG = "DAPI-ChatService";

	/**
     * Constructor
     * 
     * @param ctx Application context
     * @param listener Service listener
     */
    public ChatService(Context ctx, RCSServiceListener listener) {
    	super(ctx, listener);
    }

    /**
     * Connects to the API
     */
    public void connect() {
    	Log.i(TAG, "connect() entry");
    }
    
    /**
     * Disconnects from the API
     */
    public void disconnect() {
        Log.i(TAG, "disconnect() entry");
    }

    
    /**
     * Init a single chat with a given contact and returns a Chat instance.
     * The parameter contact supports the following formats: MSISDN in national
     * or international format, SIP address, SIP-URI or Tel-URI.
     * 
     * @param contact Contact
     * @param listener Chat event listener
     * @return Chat or null 
     * @throws RCSServiceNotAvailableException
     */
    public Chat initSingleChat(String contact, ChatListener listener) throws RCSServiceNotAvailableException {
    	Log.i(TAG, "initSingleChat entry " + contact);
		return null;
    }

    /**
     * Returns a chat in progress with a given contact
     * 
     * @param contact Contact
     * @return Chat or null if not found
     * @throws RCSServiceNotAvailableException
     */
    public Chat getChat(String chatId) throws RCSServiceNotAvailableException {
    	Log.i(TAG, "getChat entry " + chatId);
		if (api != null) {
            return null;
		} else {
			throw new RCSServiceNotAvailableException();
		}
    }

    
    /**
	 * Registers a chat invitation listener
	 * 
	 * @param listener New chat listener
	 * @throws RCSServiceNotAvailableException
	 */
	public void addEventListener(ChatListener listener) throws RCSServiceNotAvailableException {
		Log.i(TAG, "addEventListener entry" + listener);
	}

	/**
	 * Unregisters a chat invitation listener
	 * 
	 * @param listener New chat listener
	 * @throws RCSServiceNotAvailableException
	 */
	public void removeChatListener(ChatListener listener) throws RCSServiceNotAvailableException {
		Log.i(TAG, "removeEventListener entry" + listener);
    }

 /**
     * Sends a chat message t
     * 
     * @param Contact contact
     * @param Message message
     * @return Unique message ID or null in case of error
     * @throws RCSServiceNotAvailableException
     */
	public String sendMessage(String Contact,String message) throws RCSServiceNotAvailableException  {
		Log.i(TAG, "sendMessage Contact " + Contact+ " message "+message);
		if (api != null) {
		} else {
			throw new RCSServiceNotAvailableException();
		}
		return null;
	}

    /**
     * get chat message
     * 
     * @param Message message
     * @return Unique message ID or null in case of error
     * @throws RCSServiceNotAvailableException
     */	
	public ChatMessage getChatMessage(String msgId) throws RCSServiceNotAvailableException  {
		Log.i(TAG, "getChatMessage msgId " + msgId);
		if (api != null) {
		} else {
			throw new RCSServiceNotAvailableException();
		}
		return null;
	}

    /**
     * Sends a chat message to multi contacts
     * 
     * @param Contacts contacts
     * @param Message message
     * @return Unique message ID or null in case of error
     * @throws RCSServiceNotAvailableException
     */
	public String sendOTMMessage(Set<String> Contacts,String message) throws RCSServiceNotAvailableException  {
		Log.i(TAG, "sendOTMMessage Contacts " + Contacts+ " message "+message);
		if (api != null) {
		} else {
			throw new RCSServiceNotAvailableException();
		}
		return null;
	}
	
    /**
     * re-send message
     * 
     * @param Message message
     * @return Unique message ID or null in case of error
     * @throws RCSServiceNotAvailableException
     */	
	public String resendMessage(String msgId) throws RCSServiceNotAvailableException  {
		Log.i(TAG, "resendMessage msgId " + msgId);
		if (api != null) {
		} else {
			throw new RCSServiceNotAvailableException();
		}
		return null;
	}
	
    /**
     * delete message
     * 
     * @param Message message
     * @return if success ,return true; or not ,return false
     * @throws RCSServiceNotAvailableException
     */	
	public boolean deleteMessage(String msgId) throws RCSServiceNotAvailableException  {
		Log.i(TAG, "resendMessage msgId " + msgId);
		if (api != null) {
		} else {
			throw new RCSServiceNotAvailableException();
		}
		return true;
	}
	
    /**
     * set read message
     * 
     * @param Message message
     * @return if success ,return true; or not ,return false
     * @throws RCSServiceNotAvailableException
     */	
	public boolean setMessageRead(String msgId) throws RCSServiceNotAvailableException  {
		Log.i(TAG, "setMessageRead msgId " + msgId);
		if (api != null) {
		} else {
			throw new RCSServiceNotAvailableException();
		}
		return true;
	}
	
    /**
     * set favorite message
     * 
     * @param Message message
     * @return if success ,return true; or not ,return false
     * @throws RCSServiceNotAvailableException
     */	
	public boolean setMessageFavorite(String msgId) throws RCSServiceNotAvailableException  {
		Log.i(TAG, "setMessageFavorite msgId " + msgId);
		if (api != null) {
		} else {
			throw new RCSServiceNotAvailableException();
		}
		return true;
	}
	
    /**
     * move message to Inbox
     * 
     * @param Message message
     * @return if success ,return true; or not ,return false
     * @throws RCSServiceNotAvailableException
     */	
	public boolean moveMessagetoInbox(String msgId) throws RCSServiceNotAvailableException  {
		Log.i(TAG, "moveMessagetoInbox msgId " + msgId);
		if (api != null) {
		} else {
			throw new RCSServiceNotAvailableException();
		}
		return true;
	}

}

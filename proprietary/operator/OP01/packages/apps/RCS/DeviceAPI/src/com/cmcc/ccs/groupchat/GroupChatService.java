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
public class GroupChatService extends RCSService {
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
    public GroupChatService(Context ctx, RCSServiceListener listener) {
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
     * Initiates a group chat with a group of contact and returns a GroupChat
     * instance. The subject is optional and may be null.
     * 
     * @param contact List of contacts
     * @param subject Subject
     * @param listener Chat event listener
     * @throws RCSServiceNotAvailableException
     */
    public GroupChat initiateGroupChat(Set<String> contacts, String subject, GroupChatListener listener) throws RCSServiceNotAvailableException {
    	Log.i(TAG, "initiateGroupChat entry= " + contacts + " subject =" + subject );
    	return null;
    }

    
    /**
     * Rejoins an existing group chat from its unique chat ID
     * 
     * @param chatId Chat ID
     * @return Group chat
     * @throws RCSServiceNotAvailableException
     * @hide
     */
    public GroupChat rejoinGroupChat(String chatId) throws RCSServiceNotAvailableException {
    	Log.i(TAG, "rejoinGroupChat entry= " + chatId );
		if (api != null) {
            return null;
		} else {
			throw new RCSServiceNotAvailableException();
		}
    }
    
    /**
     * Restarts a previous group chat from its unique chat ID
     * 
     * @param chatId Chat ID
     * @return Group chat
     * @throws RCSServiceNotAvailableException
     */
    public GroupChat restartGroupChat(String chatId) throws RCSServiceNotAvailableException {
    	Log.i(TAG, "restartGroupChat entry= " + chatId );
		if (api != null) {
            return null;
		} else {
			throw new RCSServiceNotAvailableException();
		}
    }
        


       
    /**
     * Returns a group chat in progress from its unique ID
     * 
     * @param chatId Chat ID
     * @return Group chat or null if not found
     * @throws RCSServiceNotAvailableException
     */
    public GroupChat getGroupChat(String chatId) throws RCSServiceNotAvailableException {
    	Log.i(TAG, "getGroupChat entry " + chatId);
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
	public void addChatListener(NewGroupChatListener listener) throws RCSServiceNotAvailableException {
		Log.i(TAG, "addEventListener entry" + listener);
	}

	/**
	 * Unregisters a chat invitation listener
	 * 
	 * @param listener New chat listener
	 * @throws RCSServiceNotAvailableException
	 */
	public void removeChatListener(NewGroupChatListener listener) throws RCSServiceNotAvailableException {
		Log.i(TAG, "removeEventListener entry" + listener);
    }

    
	
	
	
	
}

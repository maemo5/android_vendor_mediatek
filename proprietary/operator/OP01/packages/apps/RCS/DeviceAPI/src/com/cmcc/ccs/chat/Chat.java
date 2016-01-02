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

import com.cmcc.ccs.RCSServiceNotAvailableException;

import android.util.Log;

import java.util.List;
import java.util.Set;


/**
 * Chat
 * 
 * @author Jean-Marc AUFFRET
 */
public class Chat {
    /**
     * Chat interface
     */
    public static final String TAG = "DAPI-Chat";
    private Set<String> mContacts;
    
    /**
     * Constructor
     * 
     * @param chatIntf Chat interface
     */
    Chat(Set<String> contacts) {
        mContacts = contacts;
    }

    /**
     * Returns the remote contact
     * 
     * @return Contact
     * @throws RCSServiceNotAvailableException
     * @hide
     */
    public Set<String> getRemoteContact() throws RCSServiceNotAvailableException {
    	Log.i(TAG, "getRemoteContact entry");
		return mContacts;
    }
    
	/**
        * Sends a chat message
        * 
        * @param message Message
        * @return Unique message ID or null in case of error
        * @throws RCSServiceNotAvailableException
        * @hide
     */
    public String sendMessage(String message) throws RCSServiceNotAvailableException {
    	Log.i(TAG, "ABC sendMessage entry " + message);
		return null; 	
    }


    /**
     * Sends a chat message to the contact
     * 
     * @param Contact contact
     * @param message Message
     * @return Unique message ID or null in case of error
     * @throws RCSServiceNotAvailableException
     */
    public String sendMessage(String Contact, String message) throws RCSServiceNotAvailableException {
    	Log.i(TAG, "ABC sendMessage entry " + message);
		return null; 	
    }

     /**
     * Sends a chat message to multi contacts
     * 
     * @param Contacts contacts
     * @param message Message
     * @return Unique message ID or null in case of error
     * @throws RCSServiceNotAvailableException
     */
    public String sendOTMMessage(Set<String> Contacts, String message) throws RCSServiceNotAvailableException {
    	Log.i(TAG, "ABC sendMessage entry " + message);
		return null; 	
    }

    /**
        * reSend a chat message that failed before
        *
        * @param message Message
        * @param msgId, a Unique Message ID
        */
    public String resendMessage(String message, String msgId) throws RCSServiceNotAvailableException {
        return null;
    }

    /**
        * reSend a chat message that failed before
        *
        * @param msgId, a Unique Message ID
        * @hide
        */
    public String resendMessage(String msgId) throws RCSServiceNotAvailableException {
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
    
	
    /**
     * Adds a listener on chat events
     *  
     * @param listener Chat event listener
	 * @throws RCSServiceNotAvailableException
     */
    public void addEventListener(ChatListener listener) throws RCSServiceNotAvailableException {
    	Log.i(TAG, "addEventListener entry " + listener);
    }
	
    /**
     * Removes a listener on chat events
     * 
     * @param listener Chat event listener
	 * @throws RCSServiceNotAvailableException
     */
    public void removeEventListener(ChatListener listener) throws RCSServiceNotAvailableException {
    	Log.i(TAG, "removeEventListener entry " + listener);
    }

    /**
        * Sends a geoloc message
        * 
        * @param geoloc Geoloc info
	 * @return Unique message ID or null in case of error
   	 * @throws RCSServiceNotAvailableException
   	 * @hide
     */
//    public String sendGeoloc(Geoloc geoloc) throws RCSServiceNotAvailableException {
//    	return null; 	
//    }
    
    /**
     * Sends a burned message
     * 
     * @param message Message
     * @return Unique message ID or null in case of error
     * @throws RCSServiceNotAvailableException
     * @hide
     */
    public String sendBurnedMessage(String message) throws RCSServiceNotAvailableException {
        Log.i(TAG, "ABC sendMessage entry " + message);
        return null;    
    }
}

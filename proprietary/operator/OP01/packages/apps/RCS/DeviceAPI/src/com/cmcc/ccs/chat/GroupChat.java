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
import java.util.Set;

import com.cmcc.ccs.RCSServiceNotAvailableException;
import com.cmcc.ccs.ft.FileTransferListener;
import com.cmcc.ccs.ft.FileTransfer;


import android.util.Log;

/**
 * Group chat
 * 
 * @author Jean-Marc AUFFRET
 */
public class GroupChat {
	
	public static final String TAG = "TAPI-GroupChat";
    /**
     * Group chat state
     */
    public static class State {

  	/**
    	 * Chat invitation received
    	 */
    	public final static int INVITED = 0;
    	
  	/**
    	 * Chat invitation sent
    	 */
    	public final static int INITIATED = 1;
    	
   	/**
    	 * Chat is started
    	 */
    	public final static int STARTED = 2;
    	
   	/**
    	 * Chat has been terminated
    	 */
    	public final static int TERMINATED = 3;
    	   	
   	/**
    	 * Chat has been aborted 
    	 */
    	public final static int ABORTED = 4;
    	
   	/**
    	 * Chat has failed 
    	 */
    	public final static int FAILED = 5;
    	
        private State() {
        }    	
    }
    
    /**
     * Direction of the group chat
     */
    public static class Direction {
        /**
         * Incoming chat
         */
        public static final int INCOMING = 0;
        
        /**
         * Outgoing chat
         */
        public static final int OUTGOING = 1;
    }
    
    /**
     * Group chat error
     */
    public static class Error {
    	/**
    	 * Group chat has failed
    	 */
    	public final static int CHAT_FAILED = 0;
    	
    	/**
    	 * Group chat invitation has been declined by remote
    	 */
    	public final static int INVITATION_DECLINED = 1;

    	/**
    	 * Chat conversation not found
    	 */
    	public final static int CHAT_NOT_FOUND = 2;
    	    	
        private Error() {
        }    	
    }

    
    /**
     * Constructor
     * 
     * @param chatIntf Group chat interface
     */
    GroupChat() {

    }

    /**
     * Returns the chat ID
     * 
     * @return Chat ID
	 * @throws RCSServiceNotAvailableException
     */
	public String getChatId() throws RCSServiceNotAvailableException {
		Log.i(TAG, "getChatId entry " );
        return null;
	}

    /**
        * Returns the subject of the group chat
        * 
        * @return Subject
        * @throws RCSServiceNotAvailableException
        */
	public String getSubject() throws RCSServiceNotAvailableException {
		Log.i(TAG, "getSubject() entry.");
        return null;
	}

    /**
        * Returns the list of connected participants. A participant is identified
        * by its MSISDN in national or international format, SIP address, SIP-URI or Tel-URI.
        * 
        * @return List of participants
        * @throws RCSServiceNotAvailableException
        */
	public Set<String> getParticipants() throws RCSServiceNotAvailableException {
		Log.i(TAG, "getParticipants() entry");
        return null;
	}

    /**
        * get the chairmen of the group
        * 
        * @return chairmen string
        * @throws RCSServiceNotAvailableException
        */
	public String getChairmen() throws RCSServiceNotAvailableException {
		Log.i(TAG, "getParticipants() entry");
        return null;
	}

    /**
        * transfer the role of chairmen to other participent
        * 
        * @param contact contact
        * @throws RCSServiceNotAvailableException
        */
	public void setChairmen(String contact) throws RCSServiceNotAvailableException {
		Log.i(TAG, "getParticipants() entry");
	}

    public void modifySubject(String subject) {

    }

    /**
        * modify own nick name in group
        */
    public void modifyNickName(String nickname) {

    }

    /**
	 * Returns the state of the group chat
	 * 
	 * @return State
	 * @see GroupChat.State
	 * @throws RCSServiceNotAvailableException
	 */
	public int getState() throws RCSServiceNotAvailableException {
		Log.i(TAG, "getState() entry ");
		return 0;
	}

    /**
	 * Accepts chat invitation
	 *  
	 * @throws RCSServiceNotAvailableException
	 */
	public void acceptInvitation() throws RCSServiceNotAvailableException {
		Log.i(TAG, "acceptInvitation() entry ");
	}

    /**
	 * Rejects chat invitation
	 * 
	 * @throws RCSServiceNotAvailableException
	 */
	public void rejectInvitation() throws RCSServiceNotAvailableException {
		Log.i(TAG, "rejectInvitation() entry ");
	}

    /**
	 * Sends a text message to the group
	 * 
	 * @param text Message
	 * @return Unique message ID or null in case of error
	 * @throws RCSServiceNotAvailableException
	 */
	public String sendMessage(String text) throws RCSServiceNotAvailableException {
		Log.i(TAG, "sendMessage() entry ");
        return null;
	}

    /**
	 * Adds participants to a group chat
	 * 
	 * @param participants List of participants
	 * @throws RCSServiceNotAvailableException
	 */
	public void addParticipants(Set<String> participants) throws RCSServiceNotAvailableException {
		Log.i(TAG, "addParticipants() entry " + participants );
	}

    /**
	 * Quits a group chat conversation. The conversation will continue between
	 * other participants if there are enough participants.
	 * 
	 * @throws RCSServiceNotAvailableException
	 */
	public void quitConversation() throws RCSServiceNotAvailableException {
		Log.i(TAG, "quitConversation() entry ");
    }

    /**
	 * remove participants to a group chat
	 * 
	 * @param participants List of participants
	 * @throws RCSServiceNotAvailableException
	 */
	public void removeParticipants(Set<String> participants) throws RCSServiceNotAvailableException {
		Log.i(TAG, "removeParticipants() entry " + participants );
	    if (!isChairmen()) {
            return; 
        }
	}

    /**
	 * return wether me is chairmen
	 * 
	 * @throws RCSServiceNotAvailableException
	 */

    public boolean isChairmen() throws RCSServiceNotAvailableException {
		Log.i(TAG, "isChairmen() entry ");
        return false;
	}

    /**
	 * get the last portrait of the participants. after get them, will callback 
	 * by GroupChatListener.onPortTraitUpdate
	 * 
	 * @param participants List of participants
	 * @throws RCSServiceNotAvailableException
	 */
    public void getPortrait(Set<String>participants) throws RCSServiceNotAvailableException {
        Log.i(TAG, "getPortrait() entry ");
    }

    /** 
        * dissolve the groupchat, only chairmen can do it
        */
    public void abortConversation () throws RCSServiceNotAvailableException {
        Log.i(TAG, "abortConversation() entry ");
        if (!isChairmen()) {
            return;
        }
    }

    /**
	 * Adds a listener on chat events
	 * 
	 * @param listener Group chat event listener 
	 * @throws RCSServiceNotAvailableException
	 */
	public void addEventListener(GroupChatListener listener) throws RCSServiceNotAvailableException {
		Log.i(TAG, "addEventListener() entry " + listener);
	}
	
	/**
	 * Removes a listener on chat events
	 * 
	 * @param listener Group chat event listener 
	 * @throws RCSServiceNotAvailableException
	 */
	public void removeEventListener(GroupChatListener listener) throws RCSServiceNotAvailableException {
		Log.i(TAG, "removeEventListener() entry " + listener);
	}

	/**
        * Returns the chat ID
        *  
        * @return Chat ID
	  * @throws RCSServiceNotAvailableException
	  * @hide
     */
	public String getChatSessionId() throws RCSServiceNotAvailableException {
		Log.i(TAG, "getChatSessionId entry ");
        return null;
	}

	/**
	 * Returns the direction of the group chat (incoming or outgoing)
	 * 
	 * @return Direction
	 * @see GroupChat.Direction
	 * @throws RCSServiceNotAvailableException
	 * @hide
	 */
	public int getDirection() throws RCSServiceNotAvailableException {
		Log.i(TAG, "getDirection entry ");
		try {
			return 0;
		} catch(Exception e) {
			throw new RCSServiceNotAvailableException();
		}
	}	
	
	/**
	 * Returns the list of connected participants. A participant is identified
	 * by its MSISDN in national or international format, SIP address, SIP-URI or Tel-URI.
	 * 
	 * @return List of participants
	 * @throws RCSServiceNotAvailableException
	 * @hide
	 */
	public Set<String> getAllParticipants() throws RCSServiceNotAvailableException {
		Log.i(TAG, "getAllParticipants() entry ");
        return null;
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
//    	Log.i(TAG, "sendGeoloc() entry " + geoloc );
//    }	

    /**
        * Transfers a file to participants. The parameter filename contains the complete
        * path of the file to be transferred.
        * 
        * @param filename Filename to transfer
        * @param fileicon Filename of the file icon associated to the file to be transfered
        * @param listener File transfer event listener
        * @return File transfer
        * @throws RCSServiceNotAvailableException
        * @hide
        */
    public FileTransfer sendFile(String filename, String fileicon, FileTransferListener listener) throws RCSServiceNotAvailableException {
    	Log.i(TAG, "sendFile() entry filename=" + filename + " fileicon="+fileicon + " listener ="+listener);
        return null;
	}	
    

	/**
	 * Returns the max number of participants in the group chat. This limit is
	 * read during the conference event subscription and overrides the provisioning
	 * parameter.
	 * 
	 * @return Number
	 * @throws RCSServiceNotAvailableException
	 * @hide
	 */
	public int getMaxParticipants() throws RCSServiceNotAvailableException {
		Log.i(TAG, "getMaxParticipants() entry ");
        return 0;
	}
}

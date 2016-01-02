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

import java.util.Date;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import java.util.Set;

/**
 * Chat message
 * 
 * @author Jean-Marc AUFFRET
 */
public class ChatMessage implements Parcelable {
	/**
	 * MIME type
	 */
	public final static String MIME_TYPE = "text/plain"; 
	
	public static final String TAG = "DAPI-ChatMessage";
	/**
	 * Unique message Id
	 */
	private String id;

	/**
	 * Contact who has sent the message
	 */
	private String contact;
	
	/**
	 * Message content
	 */
	private String message;
	
	/**
	 * Receipt date of the message
	 */
	private Date receiptAt;

	/**
	 * Display Name
	 */
	private String displayName;

	/**
	 * Contacts who has sent the message
	 */
    private Set<String> mContacts;
    
    /**
     * Constructor for outgoing message
     * 
     * @param messageId Message Id
     * @param contact Contact
     * @param message Message content
     * @param receiptAt Receipt date
     * @param displayName displayName
     * @hide
	 */
	public ChatMessage(String messageId, String remote, String message, Date receiptAt, String displayName) {
		Log.i(TAG, "ChatMessage entry" + "messageId=" + messageId + " remote=" + remote + " message=" + message + 
				" receiptAt=" + receiptAt);
		this.id = messageId;
		this.contact = remote;
		this.message = message;
		this.receiptAt = receiptAt;
		this.displayName = displayName;
	}
	
	/**
	 * Constructor
	 * 
	 * @param source Parcelable source
     * @hide
	 */
	public ChatMessage(Parcel source) {
		this.id = source.readString();
		this.contact = source.readString();
		this.message = source.readString();
		this.receiptAt = new Date(source.readLong());
		this.displayName = source.readString();
    }
	
	/**
	 * Describe the kinds of special objects contained in this Parcelable's
	 * marshalled representation
	 * 
	 * @return Integer
     * @hide
	 */
	public int describeContents() {
        return 0;
    }

	/**
	 * Write parcelable object
	 * 
	 * @param dest The Parcel in which the object should be written
	 * @param flags Additional flags about how the object should be written
     * @hide
	 */
    public void writeToParcel(Parcel dest, int flags) {
    	dest.writeString(id);
    	dest.writeString(contact);
    	dest.writeString(message);
    	dest.writeLong(receiptAt.getTime());
		dest.writeString(displayName);
    }

    /**
     * Parcelable creator
     * 
     * @hide
     */
    public static final Parcelable.Creator<ChatMessage> CREATOR
            = new Parcelable.Creator<ChatMessage>() {
        public ChatMessage createFromParcel(Parcel source) {
            return new ChatMessage(source);
        }

        public ChatMessage[] newArray(int size) {
            return new ChatMessage[size];
        }
    };	
	
	/**
	 * Returns the message ID
	 * 
	 * @return ID
	 */
    public String getId(){
    	return id;
    }

	/**
	 * Returns the contact
	 * 
	 * @return Contact
	 */
	public String getContact() {
		return contact;
	}

	/**
	 * Returns the message content
	 * 
	 * @return String
	 * @hide
	 */

	public String getMessage() {
		return message;
	}

	/**
	 * Returns the display name
	 * 
	 * @return String
	 * @hide
	 */
	public String getDisplayName() {
		return displayName;
	}
	
	/**
	 * Returns the receipt date
	 * 
	 * @return Date
	 */
	public Date getReceiptDate() {
		return receiptAt;
	}

	/**
	 * Returns the contacts
	 * 
	 * @return Contacts
	 */
	public Set<String> getContacts() {
		return mContacts;
	}
	
}

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
package com.cmcc.ccs.ft;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.cmcc.ccs.RCSService;
import com.cmcc.ccs.RCSServiceListener;

import com.cmcc.ccs.RCSServiceNotAvailableException;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.IInterface;

import android.util.Log;


/**
 * This class offers the main entry point to transfer files and to
 * receive files. Several applications may connect/disconnect to the API.
 * 
 * The parameter contact in the API supports the following formats:
 * MSISDN in national or international format, SIP address, SIP-URI
 * or Tel-URI.
 * 
 * @author Jean-Marc AUFFRET 
 */
public class FileTransferService extends RCSService {
	/**
	 * API
	 */
	
	public static final String TAG = "DAPI-FileTransferService";
	
    /**
     * Constructor
     * 
     * @param ctx Application context
     * @param listener Service listener
     */
    public FileTransferService(Context ctx, RCSServiceListener listener) {
    	super(ctx, listener);
    }

    /**
     * Connects to the API
     */
    public void connect() {
    	Log.i(TAG, "FileTransfer connect() entry");    	
    }
    
    /**
     * Disconnects from the API
     */
    public void disconnect() {

    }

    /**
     * Returns the list of file transfers in progress
     * 
     * @return List of file transfers
     * @throws RCSServiceNotAvailableException
     */
    public Set<FileTransfer> getFileTransfers() throws RCSServiceNotAvailableException {
        return null;
    }

     /**
     * Returns a current file transfer from its unique ID
     * 
     * @return File transfer or null if not found
     * @throws RCSServiceNotAvailableException
     */
    public FileTransfer getFileTransfer(String transferId) throws RCSServiceNotAvailableException {
    	return null;
    } 


    

    /**
        * Transfers a file to a contact. The parameter filename contains the complete
        * path of the file to be transferred. The parameter contact supports the following
        * formats: MSISDN in national or international format, SIP address, SIP-URI or
        * Tel-URI. If the format of the contact is not supported an exception is thrown.
        * 
        * @param contact 
        * @param filename Filename to transfer
        * @param listener File transfer event listener
        * @return File transfer
        * @throws RCSServiceNotAvailableException
        */
    public FileTransfer transferFile(String contact, String filename, FileTransferListener listener) throws RCSServiceNotAvailableException {
    	return transferFile(contact, filename, null, listener);
    }    
    
    /**
     * Transfers a file to a contact. The parameter filename contains the complete
     * path of the file to be transferred. The parameter contact supports the following
     * formats: MSISDN in national or international format, SIP address, SIP-URI or
     * Tel-URI. If the format of the contact is not supported an exception is thrown.
     * 
     * @param contact 
     * @param filename Filename to transfer
     * @param fileicon Filename of the file icon associated to the file to be transfered
     * @param listener File transfer event listener
     * @return File transfer
     * @throws RCSServiceNotAvailableException
     */
    public FileTransfer transferFile(String contact, String filename, String fileicon, FileTransferListener listener) throws RCSServiceNotAvailableException {
    	return null;
	}

    /**
     * transfers a file to a group of contacts outside of a current group chat. The
     * parameter file contains the complete filename including the path to be transferred.
     * See also the method GroupChat.sendFile() of the Chat API to send a file from an
     * existing group chat conversation
     * 
     * @param set of contacts 
     * @param filename Filename to transfer
     * @param listener File transfer event listener
     * @return File transfer
     * @throws RCSServiceNotAvailableException
     */
    public FileTransfer transferFileToMultireceipt(Set<String> contacts, String filename, FileTransferListener listener) throws RCSServiceNotAvailableException
    {
    	return null;
    }
    
    /**
     * transfers a file to a group of contacts outside of a current group chat. The
     * parameter file contains the complete filename including the path to be transferred.
     * See also the method GroupChat.sendFile() of the Chat API to send a file from an
     * existing group chat conversation
     * 
     * @param set of contacts 
     * @param filename Filename to transfer
     * @param listener File transfer event listener
     * @return File transfer
     * @throws RCSServiceNotAvailableException
     */
    public FileTransfer transferFileToGroup(String chatId,Set<String> contacts, String filename, FileTransferListener listener) throws RCSServiceNotAvailableException
    {
    	return transferFileToGroup(chatId,contacts, filename, null, listener);
    }
    
    /**
     * transfers a file to a group of contacts with an optional file icon.
     * @param set of contacts 
     * @param filename Filename to transfer
     * @param fileicon Filename of the file icon associated to the file to be transfered
     * @param listener File transfer event listener
     * @return File transfer
     * @throws RCSServiceNotAvailableException
     */
    public FileTransfer transferFileToGroup(String chatId,Set<String> contacts, String filename, String fileicon, FileTransferListener listener) throws RCSServiceNotAvailableException
    {
    	return null;
    }
      
    
    /**
	 * Registers a file transfer invitation listener
	 * 
	 * @param listener New file transfer listener
	 * @throws RCSServiceNotAvailableException
	 */
	public void addNewFileTransferListener(NewFileTransferListener listener) throws RCSServiceNotAvailableException {
	
	}

	/**
	 * Unregisters a file transfer invitation listener
	 * 
	 * @param listener New file transfer listener
	 * @throws RCSServiceNotAvailableException
	 */
	public void removeNewFileTransferListener(NewFileTransferListener listener) throws RCSServiceNotAvailableException {
		
	}
}

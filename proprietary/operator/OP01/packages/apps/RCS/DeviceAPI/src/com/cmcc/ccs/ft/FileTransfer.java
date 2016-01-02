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

import com.cmcc.ccs.RCSServiceNotAvailableException;


/**
 * File transfer
 * 
 * @author Jean-Marc AUFFRET
 */
public class FileTransfer {

	public static final String TAG = "DAPI-FileTransfer";
    /**
     * File transfer state
     */
    public static class State {
    	/**
    	 * Unknown state
    	 */
    	public final static int UNKNOWN = 0;

    	/**
    	 * File transfer invitation received
    	 */
    	public final static int INVITED = 1;
    	
    	/**
    	 * File transfer invitation sent
    	 */
    	public final static int INITIATED = 2;
    	
    	/**
    	 * File transfer is started
    	 */
    	public final static int STARTED = 3;
    	
    	/**
    	 * File transfer has been transferred with success 
    	 */
    	public final static int TRANSFERRED = 4;
    	
    	/**
    	 * File transfer has been aborted 
    	 */
    	public final static int ABORTED = 5;
    	
    	/**
    	 * File transfer has failed 
    	 */
    	public final static int FAILED = 6;
    	
    	/**
    	 * File transfer has been delivered 
    	 */
    	public final static int DELIVERED = 7;


    	private State() {
        }    	
    }
    
    /**
     * Direction of the transfer
     */
    public static class Direction {
        /**
         * Incoming transfer
         */
        public static final int INCOMING = 0;
        
        /**
         * Outgoing transfer
         */
        public static final int OUTGOING = 1;
    }     
    
    /**
     * File transfer error
     */
    public static class Error {
    	/**
    	 * Transfer has failed
    	 */
    	public final static int TRANSFER_FAILED = 0;
    	
    	/**
    	 * Transfer invitation has been declined by remote
    	 */
    	public final static int INVITATION_DECLINED = 1;

    	/**
    	 * File saving has failed 
       	 */
    	public final static int SAVING_FAILED = 2;
    	
        private Error() {
        }    	
    }


    
    /**
     * Constructor
     * 
     * @param transferIntf File transfer interface
     * @hide
     */
    public FileTransfer() {

    }
    	
    	
    /**
	 * Returns the file transfer ID of the file transfer
	 * 
	 * @return Transfer ID
	 * @throws RCSServiceNotAvailableException
	 */
	public String getTransferId() throws RCSServiceNotAvailableException {
		return null;
	}
	
	/**
	 * Returns the remote contact
	 * 
	 * @return Contact
	 * @throws RCSServiceNotAvailableException
	 */
	public String getRemoteContact() throws RCSServiceNotAvailableException {
		return null;
	}
	
	/**
     * Returns the complete filename including the path of the file to be transferred
     *
     * @return Filename
	 * @throws RCSServiceNotAvailableException
     */
	public String getFileName() throws RCSServiceNotAvailableException {
		return null;
	}

	/**
     * Returns the size of the file to be transferred
     *
     * @return Size in bytes
	 * @throws RCSServiceNotAvailableException
     */
	public int getFileSize() throws RCSServiceNotAvailableException {
		return 0;
	}	

    /**
     * Returns the MIME type of the file to be transferred
     * 
     * @return Type
	 * @throws RCSServiceNotAvailableException
     */
    public String getFileType() throws RCSServiceNotAvailableException {
    	return null;
    }
    
	/**
     * Returns the complete filename including the path of the file icon
     *
     * @return Filename
	 * @throws RCSServiceNotAvailableException
     */
	public String getFileIconName() throws RCSServiceNotAvailableException {
		return null;
	}    

	/**
	 * Returns the state of the file transfer
	 * 
	 * @return State
	 * @see FileTransfer.State
	 * @throws RCSServiceNotAvailableException
	 */
	public int getState() throws RCSServiceNotAvailableException {
		return 0;
	}

    /**
	 * Aborts the file transfer
	 * 
	 * @throws RCSServiceNotAvailableException
	 */
	public void abortTransfer() throws RCSServiceNotAvailableException {

	}


	/**
	 * Pauses the file transfer
	 * 
	 * @throws RCSServiceNotAvailableException
	 */
	public void pauseTransfer() throws RCSServiceNotAvailableException {

	}
	
	/**
	 * Resumes the file transfer
	 * 
	 * @throws RCSServiceNotAvailableException
	 */
	public void resumeTransfer() throws RCSServiceNotAvailableException {

	}

	/**
	 * Adds a listener on file transfer events
	 * 
	 * @param listener Listener
	 * @throws RCSServiceNotAvailableException
	 */
	public void addEventListener(FileTransferListener listener) throws RCSServiceNotAvailableException {
		
	}
	
	/**
	 * Removes a listener from file transfer
	 * 
	 * @param listener Listener
	 * @throws RCSServiceNotAvailableException
	 */
	public void removeEventListener(FileTransferListener listener) throws RCSServiceNotAvailableException {
		
	}
}

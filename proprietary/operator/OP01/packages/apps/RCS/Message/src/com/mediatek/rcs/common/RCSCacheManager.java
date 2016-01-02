/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2012. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.rcs.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.gsma.joyn.chat.ChatLog;
import org.gsma.joyn.chat.ChatLog.Message.Status.Content;
import org.gsma.joyn.ft.FileTransferLog;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.os.RemoteException;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Threads;
import android.util.Log;

import com.mediatek.rcs.common.binder.RCSServiceManager;
import com.mediatek.rcs.common.provider.ThreadMapCache;
import com.mediatek.rcs.common.provider.ThreadMapCache.MapInfo;
import com.mediatek.rcs.common.service.FileStruct;
import com.mediatek.rcs.common.utils.ContextCacher;
import com.mediatek.rcs.common.utils.Logger;
import com.mediatek.rcs.common.utils.RCSUtils;
import com.mediatek.rcs.common.service.IRCSChatService;
import android.media.MediaFile;
import android.text.TextUtils;
import java.io.File;

import com.mediatek.rcs.common.MessageStatusUtils.IFileTransfer.Status;


public class RCSCacheManager{
    public static Map<Long, IpMessage> sCachedSendMessage = new ConcurrentHashMap<Long, IpMessage>();
    private static final String TAG = "RCSCacheManager";

    private RCSCacheManager() {

    }

    public static IpMessage getIpMessage(long ipmsgId) {
         Log.d(TAG, "getIpMessage() enter, ipmsgId = " + ipmsgId);
         synchronized(sCachedSendMessage) {
            if (sCachedSendMessage.containsKey(ipmsgId)) {
                Log.d(TAG, "get ft ipmsg from sCachedSendMessage ipMessage = " + sCachedSendMessage.get(Long.valueOf(ipmsgId)));
                return sCachedSendMessage.get(ipmsgId);
            } 
        }
        return null;
    }

    public static void setIpMessage(IpMessage ipMsg, long ipmsgId) {
        Log.d(TAG, "setIpMessage() enter, ipmsgId = " + ipmsgId);
        if (ipMsg != null) {
            synchronized (sCachedSendMessage) {
                if (!sCachedSendMessage.containsKey(ipmsgId)) {
                    sCachedSendMessage.put(ipmsgId, ipMsg);
                }         
            }
        }
    }

    public static void updateStatus(long ipmsgId, Status rcsStatus, int smsStatus) {
        Log.d(TAG, "updateStatus() enter, ipmsgId = " + ipmsgId + " rcsStatus = " + rcsStatus + " smsStatus " + smsStatus);
        IpMessage ipMessage;
        synchronized(sCachedSendMessage) {
            if (sCachedSendMessage.containsKey(ipmsgId)) {
                Log.d(TAG, "updateStatus ipMessageId = " + ipmsgId);
                ipMessage =  sCachedSendMessage.get(ipmsgId);
                if (ipMessage instanceof IpAttachMessage) {
                    ((IpAttachMessage)ipMessage).setRcsStatus(rcsStatus);
                    ipMessage.setStatus(smsStatus);
                    sCachedSendMessage.remove(ipmsgId);
                    sCachedSendMessage.put(ipmsgId, ipMessage);
                }
            } 
        }
    }

    public static void setFilePath(long ipmsgId, String filePath) {
        Log.d(TAG, "setFilePath() enter, ipmsgId = " + ipmsgId + " filePath = " + filePath);
        IpMessage ipMessage;
        synchronized(sCachedSendMessage) {
            if (sCachedSendMessage.containsKey(ipmsgId)) {
                Log.d(TAG, "updateStatus ipMessageId = " + ipmsgId);
                ipMessage =  sCachedSendMessage.get(ipmsgId);
                if (ipMessage instanceof IpAttachMessage) {
                    ((IpAttachMessage)ipMessage).setPath(filePath);
                    sCachedSendMessage.remove(ipmsgId);
                    sCachedSendMessage.put(ipmsgId, ipMessage);
                }
            } 
        }
    }
    public static void removeIpMessage(long ipmsgId) {
        Log.d(TAG, "removeIpMessage() enter, ipmsgId = " + ipmsgId);
        synchronized(sCachedSendMessage) {
            if (sCachedSendMessage.containsKey(ipmsgId)) {
                Log.d(TAG, "removeIpMessage , ipmsgId = " + ipmsgId);
                sCachedSendMessage.remove(ipmsgId);
            }
        } 
    }

    public static void clearCache() {
        Log.d(TAG, "clearCache() enter ");
        if (sCachedSendMessage != null) {
            synchronized(sCachedSendMessage) {
                sCachedSendMessage.clear();
            }
        } 
    }

    
}



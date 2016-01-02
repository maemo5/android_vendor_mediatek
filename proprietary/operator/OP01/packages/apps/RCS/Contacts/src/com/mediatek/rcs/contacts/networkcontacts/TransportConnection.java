/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
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

package com.mediatek.rcs.contacts.networkcontacts;

import android.util.Log;

import com.mediatek.gba.GbaCredentials;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;

/**
 * @author MTK80963
 *
 */
public class TransportConnection {
    private static final String TAG = "NETWORKCONTACTS:TransportConnection Apache";

    private DefaultHttpClient mClient;
    private String mUrl = null;
    private HttpPost mPost = null;
    private HttpResponse mResponse = null;
    private InputStream mInput = null;
    /**
     * Constructor.
     */
    public TransportConnection() {

    }

    /**
     * @param uri url
     * @param proxyType
     *            : 0 -- DIRECT, 1 -- PROXY(HTTP??), 2 --SOCKS
     * @param proxyAddr proxy address
     * @param proxyPort proxy port.
     * @return result.
     */
    public boolean initialize(String uri, int proxyType, String proxyAddr,
            int proxyPort) {
        Log.d(TAG, "initialize: uri=" + uri + ", proxyType=" + proxyType
                + ", proxyAddr=" + proxyAddr + ", proxyPort=" + proxyPort);
        switch (proxyType) {
        case 0:
            break;
        case 1:
            Log.e(TAG, "Do not support HTTP proxy.");
            return false;
        case 2:
            Log.e(TAG, "Do not support SOCK5 proxy.");
            return false;
        default:
            return false;
        }

        mClient = new DefaultHttpClient();
        mUrl = uri;
        mPost = new HttpPost(mUrl);
        return true;
    }

    /**
     * @return open result.
     */
    public boolean openComm() {
        Log.d(TAG, "openComm()");
        if (mClient == null || mPost == null) {
            Log.e(TAG, "openComm: mClient=" + mClient + ", mPost=" + mPost);
            return false;
        }

        ContactsSyncEngine engine = ContactsSyncEngine.getInstance(null);
        if (engine == null) {
            Log.e(TAG, "openComm: no engine instance");
            return false;
        }
        Credentials credentials = new GbaCredentials(engine.getContext(), mUrl);
        URL serverUrl;
        try {
            serverUrl = new URL(mUrl);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;
        }
        AuthScope scope = new AuthScope(serverUrl.getHost(), serverUrl.getPort());
        BasicCredentialsProvider provider = new BasicCredentialsProvider();
        provider.setCredentials(scope, credentials);
        mClient.setCredentialsProvider(provider);

        mClient.getParams().setParameter("http.socket.timeout",
                Integer.valueOf(120 * 1000)); // read timeout
        mClient.getParams().setParameter("http.connection.timeout",
                Integer.valueOf(120 * 1000)); //connection timeout
        /* general header */
        mPost.addHeader("Accept-Encoding", "identity");
        mPost.addHeader("Cache-Control", "private");
        mPost.addHeader("Connection", "close");
        mPost.addHeader("Accept",
                "application/vnd.syncml+xml, application/vnd.syncml+wbxml, */*");
        mPost.addHeader("Accept-Language", "en");
        mPost.addHeader("Accept-Charset", "utf-8");

        return true;
    }

    /**
     * @return result
     */
    public boolean closeComm() {
        Log.d(TAG, "closeComm()");
        if (mClient == null) {
            Log.e(TAG, "closeComm: mClient=" + mClient);
            return false;
        }

        mClient = null;
        mPost = null;
        mUrl = null;
        mResponse = null;
        mInput = null;
        return true;
    }

    /**
     * destroy connection.
     */
    public void destroy() {
        // nothing to do
    }

    /**
     * @param field property field.
     * @param value propert value.
     * @return result
     */
    public boolean addRequestProperty(String field, String value) {
        Log.d(TAG, "addRequestProperty: " + field + " = " + value);

        if (mPost == null) {
            Log.e(TAG, "AddRequestProperty: mPost=" + mPost);
            return false;
        }

        /* httpClient will add Content-length automatically */
        if (field.equalsIgnoreCase("Content-Length")) {
            return true;
        }
        mPost.setHeader(field, value);
        return true;
    }

    /**
     * @return URL of connection.
     */
    public String getURL() {
        Log.d(TAG, "getURL: mUrl=" + mUrl);
        return mUrl;
    }

    /**
     * @param data data to be sent.
     * @return bytes actually sent.
     */
    public int sendData(byte[] data) {
        Log.d(TAG, "sendData: len=" + data.length);
        if (mPost == null) {
            Log.e(TAG, "sendData: mPost=" + mPost);
            return -1;
        }

        //mPost.addHeader("Content-Length", String.valueOf(data.length));
        ByteArrayEntity entity = new ByteArrayEntity(data);
        mPost.setEntity(entity);
        Log.d(TAG, "sendData: return " + data.length);
        return data.length;
    }

    /**
     * @return content length.
     */
    public int getContentLength() {
        Log.d(TAG, "getContentLength()");

        if (mClient == null || mPost == null) {
            Log.e(TAG, "getContentLength: mClient=" + mClient + " ,mPost=" + mPost);
            return -1;
        }

        if (mResponse == null) {
            try {
                mResponse = mClient.execute(mPost);

            } catch (IOException ext) {
                Log.e(TAG, "exception in execute post");
                ext.printStackTrace();
                return -1;
            }
        }

        int length = (int) mResponse.getEntity().getContentLength();

        Log.d(TAG, "getContentLength() return " + length);
        return length;
    }

    /**
     * @param field field name.
     * @return value of field.
     */
    public String getHeadField(String field) {
        Log.d(TAG, "getHeadField: field=" + field);

        if (mClient == null || mPost == null) {
            Log.e(TAG, "getHeadField: mClient=" + mClient + " ,mPost=" + mPost);
            return null;
        }

        if (mResponse == null) {
            try {
                Log.d(TAG, "getHeadField: before execute");
                mResponse = mClient.execute(mPost);
                Log.d(TAG, "getHeadField: after execute");

            } catch (IOException ext) {
                Log.e(TAG, "exception in execute post");
                ext.printStackTrace();
                return null;
            }
        }

        Header[] headers = mResponse.getHeaders(field);
        Log.d(TAG, "getHeadField: after get");
        String value = null;
        if (headers != null) {
            value = headers[0].getValue();
        }

        Log.d(TAG, "getHeadField: return: " + value);
        return value;

    }

    /**
     * @param field field name.
     * @param defValue default value.
     * @return field value.
     */
    public int getHeadFieldInt(String field, int defValue) {
        Log.d(TAG, "getHeadFieldInt: field=" + field + " ,defValue=" + defValue);
        if (mClient == null || mPost == null) {
            Log.e(TAG, "getHeadFieldInt: mClient=" + mClient + " ,mPost=" + mPost);
            return defValue;
        }

        if (mResponse == null) {
            try {
                mResponse = mClient.execute(mPost);

            } catch (IOException ext) {
                Log.e(TAG, "exception in execute post");
                ext.printStackTrace();
                return defValue;
            }
        }

        Header[] headers = mResponse.getHeaders(field);
        String value = null;
        if (headers != null) {
            value = headers[0].getValue();
        }

        if (value != null) {
            try {
                return Integer.valueOf(value);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        return defValue;
    }

    /**
     * @param buffer buffer to save received data.
     * @return bytes actually received.
     */
    public int recvData(byte[] buffer) {
        Log.d(TAG, "recvData: buflen=" + buffer.length);
        if (mResponse == null) {
            Log.e(TAG, "recvData: mResponse=" + mResponse);
            return -1;
        }

        try {
            if (mInput == null) {
                mInput = mResponse.getEntity().getContent();
            }
            int ret = mInput.read(buffer);
            return ret;
        } catch (SocketTimeoutException e) {
            Log.e(TAG, "recvData: SocketTimeoutException!!");
        } catch (IllegalStateException e) {
            Log.e(TAG, "recvData: IllegalStateException!!");
            e.printStackTrace();
        } catch (IOException e) {
            Log.e(TAG, "recvData: IOException!!");
        }

        return -1;
    }
}

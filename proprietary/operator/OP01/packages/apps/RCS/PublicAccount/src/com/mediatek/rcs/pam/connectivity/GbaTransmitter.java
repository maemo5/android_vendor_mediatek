package com.mediatek.rcs.pam.connectivity;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.util.Log;

import com.mediatek.gba.GbaCredentials;
import com.mediatek.rcs.pam.CommonHttpHeader;
import com.mediatek.rcs.pam.PlatformManager;
import com.mediatek.rcs.pam.client.PAMClient.Response;
import com.mediatek.rcs.pam.client.PAMClient.Transmitter;

/**
 * PAM client with GBA support.
 */
public class GbaTransmitter implements Transmitter {
	public static final String TAG = "PAM/GbaTransmitter";
	
	private String mServerUrl;
	private Context mContext;
	private String mNafUrl;
	public GbaTransmitter(Context context, String serverUrl, String nafUrl) {
		mServerUrl = serverUrl;
		mContext = context;
		mNafUrl = nafUrl;
	}

	@Override
	public Response sendRequest(String msgname, String content, boolean postOrGet) {
		Log.d(TAG, "sendRequest: " + msgname);
		Log.d(TAG, content);
		Response result = new Response();
		try {
			URL serverUrl= new URL(mServerUrl);
			Credentials credentials = new GbaCredentials(mContext, mNafUrl);
			AuthScope authScope = new AuthScope(serverUrl.getHost(), serverUrl.getPort());
			BasicCredentialsProvider credentialsProvider=new BasicCredentialsProvider();
			credentialsProvider.setCredentials(authScope, credentials);
			DefaultHttpClient client=new DefaultHttpClient();
			client.setCredentialsProvider(credentialsProvider);
			HttpUriRequest request = null;
			if (postOrGet) {
				HttpPost post = new HttpPost(mServerUrl);
				StringEntity entity = new StringEntity(content, "UTF-8");
				entity.setContentType(CommonHttpHeader.CONTENT_TYPE_VALUE);
				post.setEntity(entity);
				request = post;
			} else {
				HttpGetWithEntity get = new HttpGetWithEntity(mServerUrl);
				StringEntity entity = new StringEntity(content, "UTF-8");
				entity.setContentType(CommonHttpHeader.CONTENT_TYPE_VALUE);
				get.setEntity(entity);
				request = get;
			}
			request.setHeader(CommonHttpHeader.ACCEPT_ENCODING, CommonHttpHeader.ACCEPT_ENCODING_VALUE);
			request.setHeader(CommonHttpHeader.X_3GPP_INTENDED_IDENTITY, PlatformManager.getInstance().getIdentity(mContext));
			request.setHeader(CommonHttpHeader.USER_AGENT, "XDM-client/OMA1.0"/*CommonHttpHeader.USER_AGENT_VALUE*/);
			HttpResponse httpResponse = client.execute(request);
			StatusLine statusLine = httpResponse.getStatusLine();
			result.result = statusLine.getStatusCode();
			result.content = IOUtils.toString(httpResponse.getEntity().getContent(), "UTF-8");
			Log.d(TAG, "Status Line: " + statusLine);
			Log.d(TAG, "Status Code: " + result.result);
			Log.d(TAG, "Response Content: " + result.content);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			result.result = -1;
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			result.result = -1;
		} catch (IOException e) {
			e.printStackTrace();
			result.result = -1;
		}
		return result;
	}
}

package com.mediatek.rcs.pam.client;

import java.util.List;

import android.content.Context;

import com.mediatek.rcs.pam.PAMException;
import com.mediatek.rcs.pam.PlatformManager;
import com.mediatek.rcs.pam.model.MessageContent;
import com.mediatek.rcs.pam.model.PublicAccount;

/**
 * This is a facade of PAClient for UI components such as activities. By using this
 * object, activities can retrieve non-persistent data from server without bothering
 * PAService and/or content provider.
 * 
 * The APIs here are sync method invocations. So watch out for ANRs.
 */
public class UIPAMClient {

	private PAMClient mClient;
	public UIPAMClient(Context context) {
		PlatformManager pm = PlatformManager.getInstance();
		mClient = new PAMClient(
				pm.getTransmitter(context),
				context);
	}

	/*
	 * Operation: subscribe
	 * Exposed: no
	 * Reasoning: It is not directly exposed to activities and can be only accessed via 
	 *            PAService. We decide to hide this API because it will modify data in 
	 *            the content provider. Activities can retrieve the data interested via
	 *            content provider later.
	 */
	
	/*
	 * Operation: unsubscribe
	 * Exposed: no
	 * Reasoning: It is not directly exposed to activities and can be only accessed via 
	 *            PAService. We decide to hide this API because it will modify data in 
	 *            the content provider. Activities can retrieve the data interested via
	 *            content provider later.
	 */
	
	/*
	 * Operation: getSubscribedList
	 * Exposed: no
	 * Reasoning: It is not directly exposed to activities and can be only accessed via 
	 *            PAService. We decide to hide this API because it will modify data in 
	 *            the content provider. Activities can retrieve the data interested via
	 *            content provider later.
	 */
	
	/*
	 * Operation: search
	 * Exposed: yes
	 * Reasoning: It can be invoked directly by activities because the data it retrieved
	 *            from the server are used only temporarily. We can avoid storing and
	 *            retrieving these temporary data by allowing activities to invoked it. 
	 */
	public List<PublicAccount> search(String keyword, int order, int pageSize, int pageNumber) throws PAMException {
		return mClient.search(keyword, order, pageSize, pageNumber);
	}
	
	/*
	 * Operation: getDetails
	 * Exposed: no
	 * Reasoning: It is not directly exposed to activities and can be only accessed via 
	 *            PAService. We decide to hide this API because we will cache the details 
	 *            data in the content provider to minimize networking. PAService is
	 *            responsible for clearing the cached data. 
	 */

	/*
	 * Operation: getMenu
	 * Exposed: no
	 * Reasoning: It is not directly exposed to activities and can be only accessed via 
	 *            PAService. We decide to hide this API because it will modify data in 
	 *            the content provider. Activities can retrieve the data interested via
	 *            content provider later.
	 */

	/*
	 * Operation: getMessageHistory
	 * Exposed: Yes
	 * Reasoning: It is exposed directly to activities because it does not affect the
	 *            data in content provider.
	 */
	public List<MessageContent> getMessageHistory(String uuid, String timestamp, int order, int pageSize, int pageNumber) throws PAMException {
		return mClient.getMessageHistory(uuid, timestamp, order, pageSize, pageNumber);
	}

	/*
	 * Operation: complain
	 * Exposed: no
	 * Reasoning: It can be invoked directly by activities because it does not affect the
	 *            data in content provider.
	 */
	public int complain(String uuid, int type, String reason, String data, String description) throws PAMException {
		return mClient.complain(uuid, type, reason, data, description);
	}
	
	/*
	 * Operation: getRecommands
	 * Exposed: yes
	 * Reasoning: It can be invoked directly by activities because the data it retrieved
	 *            from the server are used only temporarily. We can avoid storing and
	 *            retrieving these temporary data by allowing activities to invoked it. 
	 */
	public List<PublicAccount> getRecommends(int type, int pageSize, int pageNumber) throws PAMException {
		return mClient.getRecommends(type, pageSize, pageNumber);
	}

	/*
	 * Operation: setAcceptStatus
	 * Exposed: no
	 * Reasoning: It is not directly exposed to activities and can be only accessed via 
	 *            PAService. We decide to hide this API because it will modify data in 
	 *            the content provider. Activities can retrieve the data interested via
	 *            content provider later.
	 */

	
}

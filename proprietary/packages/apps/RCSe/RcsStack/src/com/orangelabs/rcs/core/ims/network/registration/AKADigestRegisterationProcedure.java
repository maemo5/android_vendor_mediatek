package com.orangelabs.rcs.core.ims.network.registration;

import java.util.Arrays;

import javax2.sip.header.AuthorizationHeader;
import javax2.sip.header.WWWAuthenticateHeader;

import android.util.Base64;
import com.orangelabs.rcs.core.CoreException;
import com.orangelabs.rcs.core.ims.ImsModule;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.protocol.sip.SipResponse;
import com.orangelabs.rcs.core.ims.security.AKADigestAuthentication;
import com.orangelabs.rcs.provider.settings.RcsSettings;

public class AKADigestRegisterationProcedure extends RegistrationProcedure {
	
	/**
	 * AKA Digest MD5 agent , Comment to use SHA TODO set bases on configuration
	 */
	private AKADigestAuthentication digest = null;
	
	private String password = "";

	private final static int RAND_LEN = 16;
	private final static int AUTN_LEN = 16;

	
	private String algorithm = "AKAv1-MD5";
	private String user = "";
	private String uri = null;
	private String uriSet= null;
	private String response = "";


	private static String TAG = "AKADigestRegisterationProcedure";
	/**
	 * Cnonce counter
	 */
	private int nc = 0;

	@Override
	public void init() {
		
		digest = new AKADigestAuthentication();
		uriSet= "sip:" + ImsModule.IMS_USER_PROFILE.getUsername() + "@"
        + ImsModule.IMS_USER_PROFILE.getHomeDomain();
		uri= "sip:" + ImsModule.IMS_USER_PROFILE.getUsername();
		RcsSettings.getInstance().setPublicUri(uriSet);
	}



	@Override
	public String getHomeDomain() {
		return ImsModule.IMS_USER_PROFILE.getHomeDomain();

	}

	@Override
	public String getPublicUri() {
		// TODO Auto-generated method stub
		return "sip:" + ImsModule.IMS_USER_PROFILE.getUsername() + "@"
				+ ImsModule.IMS_USER_PROFILE.getHomeDomain();
	}

	@Override
	public void writeSecurityHeader(SipRequest request) throws CoreException {
		try {
			
			if (password != "") {
				response = digest.calculateResponse(user, password,request.getMethod(), request.getRequestURI(),Integer.toString(nc) ,null );
				nc++;
			}

			nc = 1;
			
			// Build the Authorization header
        String auth ="Digest username=\""+ ImsModule.IMS_USER_PROFILE.getPrivateID() + "\""+
			        ",realm=\"" + digest.getRealm() + "\"" + 
				    ",nonce=\"" + digest.getNonce() + "\"" +
				    ",algorithm=\""+ digest.getAlgorithm() + "\"" +
					",uri=\"" + uri + "\""+
					",response=\"" + response + "\"" + 
					",qop="+digest.getQop() +
					",nc="+digest.buildNonceCounter() +
					",cnonce=\""+digest.getCnonce()+"\""+
					",opaque=\""+digest.getOpaque() +"\"";

			// Set header in the SIP message
			request.addHeader(AuthorizationHeader.NAME, auth);
		} catch (Exception e) {

			throw new CoreException("Can't write the security header");
		}

	}

	/*1. A shared secret K is established beforehand between the ISIM and the Authentication Center (AuC).  
	 * The secret is stored in theISIM, which resides on a smart card like, tamper resistant device.*/
	
	//  Authentication Center.  The network element in mobile networks that can authorize users either in GSM or in UMTS networks.
	
	/*   2. The AuC of the home network produces an authentication vector AV,
based on the shared secret K and a sequence number SQN.  The
authentication vector contains a random challenge RAND, network
authentication token AUTN, expected authentication result XRES, a
session key for integrity check IK, and a session key for
encryption CK.*/
	
	//From 1. sim has the secret , so we need telephony for 3 see digestMD5Authentication
	
	/*4. The server creates an authentication request, which contains the
      random challenge RAND, and the network authenticator token AUTN.*/

	@Override
	public void readSecurityHeader(SipResponse response) throws CoreException {

		WWWAuthenticateHeader wwwHeader = (WWWAuthenticateHeader) response
				.getHeader(WWWAuthenticateHeader.NAME);
		try {
		    user = ImsModule.IMS_USER_PROFILE.getPrivateID();
			digest.setNonce(wwwHeader.getNonce());
			digest.setOpaque(wwwHeader.getOpaque());
			digest.setRealm( wwwHeader.getRealm()); 
		    digest.setQop(wwwHeader.getQop());
		    digest.setAlgorithm(wwwHeader.getAlgorithm());
			byte[] decodedData = Base64.decode(digest.getNonce(), Base64.DEFAULT);
			if (decodedData.length < RAND_LEN + AUTN_LEN) {
				throw new IllegalStateException(
						"The length of decoded content is less then required.");
			}
			

			
			// Split nonce into RAND (Length: 16) + AUTN (Length: 16) two
			// fields.
			final byte[] rand = new byte[RAND_LEN];
			final byte[] autn = new byte[AUTN_LEN];

			System.arraycopy(decodedData, 0, rand, 0, RAND_LEN);
			System.arraycopy(decodedData, RAND_LEN, autn, 0, AUTN_LEN);

			byte[] akaResponse = digest.calculateAkaAuthAndRes(rand, autn, 0);
			byte[] bresp = null;
			if (akaResponse != null) {
				int resLen = (int) akaResponse[1];
				bresp = new byte[resLen];
				bresp = Arrays.copyOfRange(akaResponse, 2, 2 + resLen);				
				/*  The resulting AKA RES parameter is treated as a "password" when calculating the response directive of RFC 2617.*/
				password = bytesToHex(bresp);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	final protected static char[] hexArray = "0123456789abcdef".toCharArray();

	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];

		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}

		return new String(hexChars);
	}



	@Override
	public String getPublicUri_ex() {
	    return ImsModule.IMS_USER_PROFILE.getUsername_full();

	}
	

}


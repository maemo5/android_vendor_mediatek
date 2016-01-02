package com.mediatek.rcs.pam.connectivity;

import java.net.URI;

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;

public class HttpGetWithEntity extends HttpEntityEnclosingRequestBase {
	public final static String METHOD_NAME = "GET";
	
	public HttpGetWithEntity(final String uri) {
		super();
		setURI(URI.create(uri));
	}

	@Override
	public String getMethod() {
		return METHOD_NAME;
	}

}
